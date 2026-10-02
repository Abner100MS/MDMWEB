
let modalDispositivos;
let tablets = [];
let modalTarea;



function iniciarTareas() {

    modalTarea = new bootstrap.Modal(
        document.getElementById("modalTarea")
    );

    modalDispositivos = new bootstrap.Modal(
        document.getElementById("modalDispositivos")
    );

    document.getElementById("btnNuevaTarea").onclick = abrirModalNuevaTarea;

    document.getElementById("progUnaVez")
        .addEventListener("change", cambiarTipoProgramacion);

    document.getElementById("progRepetir")
        .addEventListener("change", cambiarTipoProgramacion);

    document.getElementById("cmbFrecuencia")
        .addEventListener("change", cambiarFrecuencia);

    document.getElementById("cmbTipoTarea")
        .addEventListener("change", cambiarTipoTarea);


}

async function sincronizarTareas() {

    await cargarTareas();

}

function abrirModalNuevaTarea() {

    tareaEditando = null;

    document.getElementById("txtNombreTarea").value = "";
    document.getElementById("txtDescripcionTarea").value = "";

    document.getElementById("cmbTipoTarea").value = "REINICIO";
    document.getElementById("cmbDestinoTarea").value = "TODAS";

    cambiarTipoTarea();

    document.getElementById("txtFechaTarea").value = "";
    document.getElementById("txtHoraTarea").value = "";

    document.getElementById("txtHoraDiaria").value = "";
    document.getElementById("txtHoraSemanal").value = "";
    document.getElementById("txtHoraMensual").value = "";
    document.getElementById("txtFechaMensual").value = "";

    document.querySelectorAll(".dias-semana .btn-check")
        .forEach(x => x.checked = false);

    document.getElementById("progUnaVez").checked = true;
    document.getElementById("progRepetir").checked = false;

    document.getElementById("cmbFrecuencia").value = "DIARIA";

    cambiarDestino();
    cambiarTipoProgramacion();
    cambiarFrecuencia();

    document.querySelector("#modalTarea .modal-title").innerHTML = `
        <i class="bi bi-calendar2-check me-2"></i>
        Nueva Tarea
    `;

    modalTarea.show();

}

async function cargarTareas() {

    const res = await fetch("/tareas");

    const tareas = await res.json();

    const tbody = document.getElementById("tablaTareas");

    tbody.innerHTML = "";

    tareas.forEach(t => {

        let estadoTexto = "";
        let estadoBadge = "";

        switch (t.estado) {

            case "PENDIENTE":
                estadoTexto = "Pendiente";
                estadoBadge = "bg-secondary";
                break;

            case "EN_PROCESO":
                estadoTexto = "En progreso";
                estadoBadge = "bg-primary";
                break;

            case "COMPLETADA":
                estadoTexto = "Completada";
                estadoBadge = "bg-success";
                break;

            case "COMPLETADA_CON_ERRORES":
                estadoTexto = "Completada parcialmente";
                estadoBadge = "bg-warning text-dark";
                break;

            case "CANCELADA":
                estadoTexto = "Cancelada";
                estadoBadge = "bg-danger";
                break;

            default:
                estadoTexto = t.estado;
                estadoBadge = "secondary";
        }

        tbody.innerHTML += `
    <tr>

        <td>${t.nombre}</td>

        <td>${t.tipoTarea}</td>

        <td>${t.destinoTarea}</td>

        <td>${t.fechaProgramada}</td>

        <td>${t.horaProgramada}</td>

        <td style="width:220px;">
            <span class="badge ${estadoBadge}">
                ${estadoTexto}
            </span>
        </td>

        <td class="text-center fw-bold" style="width:90px;">
            ${t.completados}/${t.totalDispositivos}
        </td>

        <td class="text-center" style="width:130px;">

            <button class="btn btn-sm btn-info me-1"
                onclick="verDetalleTarea(${t.id})">

                <i class="bi bi-eye"></i>

            </button>

            <button class="btn btn-sm btn-warning me-1"
                onclick="editarTarea(${t.id})">

                <i class="bi bi-pencil"></i>

            </button>

            <button class="btn btn-sm btn-danger"
                onclick="eliminarTarea(${t.id})">

                <i class="bi bi-trash"></i>

            </button>

        </td>

    </tr>
`;

    });

}

async function guardarTarea() {

    const destino = document.getElementById("cmbDestinoTarea").value;

    let valorDestino = "";
    let dispositivos = [];

    if (destino === "PLANTA") {

        valorDestino = document.getElementById("cmbPlanta").value;

    } else if (destino === "CATEGORIA") {

        valorDestino = document.getElementById("cmbCategoria").value;

    } else if (destino === "DISPOSITIVOS") {

        dispositivos = dispositivosSeleccionados;

    }

    const tipoProgramacion = document.querySelector(
        'input[name="tipoProgramacion"]:checked'
    ).value;

    let fechaProgramada = "";
    let horaProgramada = "";
    let diasSemana = "";
    let diaMes = null;
    let tipo = "UNA_VEZ";

    if (tipoProgramacion === "UNA_VEZ") {

        tipo = "UNA_VEZ";

        fechaProgramada = document.getElementById("txtFechaTarea").value;
        horaProgramada = document.getElementById("txtHoraTarea").value;

    } else {

        tipo = document.getElementById("cmbFrecuencia").value;

        switch (tipo) {

            case "DIARIA":

                fechaProgramada = new Date().toISOString().substring(0, 10);

                horaProgramada = document.getElementById("txtHoraDiaria").value;

                break;

            case "SEMANAL":

                fechaProgramada = new Date().toISOString().substring(0, 10);

                diasSemana = [...document.querySelectorAll(".dias-semana .btn-check:checked")]
                    .map(x => x.value)
                    .join(",");

                horaProgramada = document.getElementById("txtHoraSemanal").value;

                break;

            case "MENSUAL":

                fechaProgramada = new Date().toISOString().substring(0, 10);

                const fecha = document.getElementById("txtFechaMensual").value;

                diaMes = fecha ? Number(fecha.split("-")[2]) : null;

                horaProgramada = document.getElementById("txtHoraMensual").value;

                break;

        }

    }

    const tipoTarea =
        document.getElementById("cmbTipoTarea").value;

    let parametros = "";

    if (tipoTarea === "ACTUALIZAR_APP") {

        parametros =
            document.getElementById("txtUrlApk").value.trim();

        if (parametros === "") {

            Swal.fire({
                icon: "warning",
                title: "URL requerida",
                text: "Debe ingresar la URL del APK."
            });

            return;
        }
    }

    const tarea = {

        nombre: document.getElementById("txtNombreTarea").value,

        descripcion: document.getElementById("txtDescripcionTarea").value,

        tipoTarea: tipoTarea,

        destinoTarea: destino,

        valorDestino: valorDestino,

        fechaProgramada: fechaProgramada,

        horaProgramada: horaProgramada,

        tipoProgramacion: tipo,

        diasSemana: diasSemana,

        diaMes: diaMes,

        parametros: parametros,

        dispositivos: dispositivos

    };

    const url = tareaEditando == null
        ? "/tareas"
        : "/tareas/" + tareaEditando;

    const metodo = tareaEditando == null
        ? "POST"
        : "PUT";

    const res = await fetch(url, {

        method: metodo,

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify(tarea)

    });

    if (!res.ok) {

        alert("No fue posible guardar la tarea.");
        return;

    }

    modalTarea.hide();

    cargarTareas();

}

document.addEventListener("change", function (e) {

    if (e.target.id === "cmbDestinoTarea") {

        cambiarDestino();

    }

});




function cambiarDestino() {

    dispositivosSeleccionados = [];

    const destino = document.getElementById("cmbDestinoTarea").value;

    const div = document.getElementById("contenidoDestino");

    switch (destino) {

        case "TODAS":

            div.innerHTML = `
    <div class="alert alert-secondary mb-0">
        Esta tarea se ejecutará en todas las tablets.
    </div>
    `;
            break;

        case "PLANTA":

            div.innerHTML = `
                <select id="cmbPlanta" class="form-select">

                    <option value="">
                        Seleccione una planta
                    </option>

                    <option value="PC">
                        Planta Central
                    </option>

                    <option value="PF">
                        Planta Fajas
                    </option>

                </select>
            `;

            break;

        case "CATEGORIA":

            div.innerHTML = `
                <select id="cmbCategoria" class="form-select">

                    <option value="">
                        Seleccione una categoría
                    </option>

                    <option value="GENERAL">
                        General
                    </option>

                    <option value="CALIDAD">
                        Calidad
                    </option>

                    <option value="CELULAR">
                        Celular
                    </option>

                    <option value="HANDHELD">
                        Handheld
                    </option>

                    <option value="SIN DATOS">
                        Sin datos
                    </option>

                </select>
            `;

            break;

        case "DISPOSITIVOS":

            div.innerHTML = `
    <button class="btn btn-outline-primary w-100"
        onclick="abrirSelectorDispositivos()">

        Seleccionar dispositivos

    </button>

    <div id="resumenDispositivos"
        class="mt-2 text-muted">

        Ningún dispositivo seleccionado.

    </div>
    `;

            break;

    }

}

function llenarPlantas() {

    const combo =
        document.getElementById("cmbPlanta");

    if (!combo) {
        return;
    }

    combo.innerHTML = `
        <option value="">
            Seleccione una planta
        </option>

        <option value="PC">
            Planta Central
        </option>

        <option value="PF">
            Planta Fajas
        </option>
    `;
}


async function llenarCategorias() {

    const combo =
        document.getElementById("cmbCategoria");

    if (!combo) {
        return;
    }

    combo.innerHTML = `
        <option value="">
            Cargando categorías...
        </option>
    `;

    try {

        const res =
            await fetch("/devices/categorias");

        if (!res.ok) {
            throw new Error(
                "No se pudieron cargar las categorías"
            );
        }

        const categorias =
            await res.json();

        combo.innerHTML = `
            <option value="">
                Seleccione una categoría
            </option>
        `;

        categorias.forEach(categoria => {

            combo.innerHTML += `
                <option value="${categoria}">
                    ${categoria}
                </option>
            `;
        });

    } catch (error) {

        console.error(
            "Error cargando categorías:",
            error
        );

        combo.innerHTML = `
            <option value="">
                Error al cargar categorías
            </option>
        `;
    }
}

async function eliminarTarea(id) {

    const resultado = await Swal.fire({
        title: "¿Eliminar tarea?",
        text: "Esta acción no se puede deshacer.",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "Sí, eliminar",
        cancelButtonText: "Cancelar",
        confirmButtonColor: "#dc3545",
        cancelButtonColor: "#6c757d",
        reverseButtons: true
    });

    if (!resultado.isConfirmed) {
        return;
    }

    const res = await fetch("/tareas/" + id, {
        method: "DELETE"
    });

    if (!res.ok) {

        Swal.fire({
            icon: "error",
            title: "Error",
            text: "No fue posible eliminar la tarea."
        });

        return;
    }

    Swal.fire({
        icon: "success",
        title: "Tarea eliminada",
        text: "La tarea se eliminó correctamente.",
        timer: 1500,
        showConfirmButton: false
    });

    cargarTareas();
}



async function editarTarea(id) {

    const res = await fetch("/tareas/" + id);

    const t = await res.json();

    tareaEditando = id;

    document.getElementById("txtNombreTarea").value = t.nombre;
    document.getElementById("txtDescripcionTarea").value = t.descripcion;
    document.getElementById("cmbTipoTarea").value = t.tipoTarea;
    document.getElementById("cmbDestinoTarea").value = t.destinoTarea;

    cambiarTipoTarea();

    if (t.tipoTarea === "ACTUALIZAR_APP") {
        document.getElementById("txtUrlApk").value =
            t.parametros || "https://github.com/Abner100MS/MonitoreoTablets/releases/download/V1.6/Monitoreo.apk";
    }


    cambiarDestino();

    setTimeout(() => {

        if (t.destinoTarea === "PLANTA") {

            document.getElementById("cmbPlanta").value = t.valorDestino;

        } else if (t.destinoTarea === "CATEGORIA") {

            document.getElementById("cmbCategoria").value = t.valorDestino;

        }

    }, 200);

    if (t.tipoProgramacion === "UNA_VEZ") {

        document.getElementById("progUnaVez").checked = true;
        document.getElementById("progRepetir").checked = false;

        cambiarTipoProgramacion();

        document.getElementById("txtFechaTarea").value = t.fechaProgramada;
        document.getElementById("txtHoraTarea").value = t.horaProgramada;

    } else {

        document.getElementById("progUnaVez").checked = false;
        document.getElementById("progRepetir").checked = true;

        cambiarTipoProgramacion();

        document.getElementById("cmbFrecuencia").value = t.tipoProgramacion;

        cambiarFrecuencia();

        switch (t.tipoProgramacion) {

            case "DIARIA":

                document.getElementById("txtHoraDiaria").value =
                    t.horaProgramada;

                break;

            case "SEMANAL":

                document.getElementById("txtHoraSemanal").value =
                    t.horaProgramada;

                document.querySelectorAll(".dias-semana .btn-check")
                    .forEach(x => x.checked = false);

                if (t.diasSemana) {

                    t.diasSemana.split(",").forEach(d => {

                        switch (d) {

                            case "LUN":
                                document.getElementById("diaLun").checked = true;
                                break;

                            case "MAR":
                                document.getElementById("diaMar").checked = true;
                                break;

                            case "MIE":
                                document.getElementById("diaMie").checked = true;
                                break;

                            case "JUE":
                                document.getElementById("diaJue").checked = true;
                                break;

                            case "VIE":
                                document.getElementById("diaVie").checked = true;
                                break;

                            case "SAB":
                                document.getElementById("diaSab").checked = true;
                                break;

                            case "DOM":
                                document.getElementById("diaDom").checked = true;
                                break;

                        }

                    });

                }

                break;

            case "MENSUAL":

                if (t.diaMes) {

                    const hoy = new Date();

                    const mes = String(hoy.getMonth() + 1).padStart(2, "0");
                    const dia = String(t.diaMes).padStart(2, "0");

                    document.getElementById("txtFechaMensual").value =
                        `${hoy.getFullYear()}-${mes}-${dia}`;
                }

                document.getElementById("txtHoraMensual").value =
                    t.horaProgramada;

                break;

        }

    }

    document.querySelector("#modalTarea .modal-title").innerHTML = `
        <i class="bi bi-calendar2-check me-2"></i>
        Editar Tarea
    `;

    modalTarea.show();

}

let plantas = [];
let categorias = [];
let dispositivosSeleccionados = [];


let paginaSelector = 0;
let totalPaginasSelector = 0;
let registrosSelector = 25;
let buscarSelector = "";
let timeoutBusquedaSelector = null;

let plantaSelector = "";
let departamentoSelector = "";
let categoriaSelector = "";
let departamentosSelector = [];


async function cargarFiltrosSelector() {

    try {

        const [resPlantas, resDepartamentos, resCategorias] =
            await Promise.all([
                fetch("/devices/plantas"),
                fetch("/devices/departamentos"),
                fetch("/devices/categorias")
            ]);

        const plantas = await resPlantas.json();
        const departamentos = await resDepartamentos.json();
        const categorias = await resCategorias.json();


        // =========================
        // PLANTAS
        // =========================

        const cmbPlanta =
            document.getElementById("cmbPlantaSelector");

        cmbPlanta.innerHTML =
            `<option value="">Todas las plantas</option>`;

        plantas.forEach(planta => {

            cmbPlanta.innerHTML += `
                <option value="${planta}">
                    ${planta}
                </option>
            `;

        });

        // =========================
        // DEPARTAMENTOS
        // =========================

        departamentosSelector = departamentos;

        mostrarDepartamentosSelector("");


        // =========================
        // CATEGORÍAS
        // =========================

        const cmbCategoria =
            document.getElementById("cmbCategoriaSelector");

        cmbCategoria.innerHTML =
            `<option value="">Todas las categorías</option>`;

        categorias.forEach(categoria => {

            cmbCategoria.innerHTML += `
                <option value="${categoria}">
                    ${categoria}
                </option>
            `;

        });

    } catch (error) {

        console.error(
            "Error cargando filtros del selector:",
            error
        );

    }
}


function mostrarDepartamentosSelector(texto) {

    const lista =
        document.getElementById("listaDepartamentosSelector");

    const busqueda = texto
        .toLowerCase()
        .trim();

    const filtrados = departamentosSelector
        .filter(departamento =>
            departamento
                .toLowerCase()
                .includes(busqueda)
        )
        .slice(0, 5); // SOLO 5 RESULTADOS

    lista.innerHTML = "";
    if (busqueda === "") {

        const todos = document.createElement("button");

        todos.type = "button";
        todos.className =
            "dropdown-item selector-opcion-departamento";

        todos.textContent = "Todos los departamentos";

        todos.onclick = async function () {

            departamentoSelector = "";

            document.getElementById(
                "btnDepartamentoSelector"
            ).textContent = "Todos los departamentos";

            bootstrap.Dropdown.getOrCreateInstance(
                document.getElementById("btnDepartamentoSelector")
            ).hide();

            paginaSelector = 0;

            await cargarDispositivosSelector();
        };

        lista.appendChild(todos);
    }

    if (filtrados.length === 0) {

        lista.innerHTML = `
            <div class="text-muted small px-2 py-2">
                No se encontraron departamentos
            </div>
        `;

        return;
    }

    filtrados.forEach(departamento => {

        const opcion = document.createElement("button");

        opcion.type = "button";
        opcion.className =
            "dropdown-item selector-opcion-departamento";

        opcion.textContent = departamento;

        opcion.onclick = function () {
            seleccionarDepartamento(departamento);
        };

        lista.appendChild(opcion);
    });
}

async function seleccionarDepartamento(departamento) {

    departamentoSelector = departamento;

    document.getElementById(
        "btnDepartamentoSelector"
    ).textContent = departamento;

    document.getElementById(
        "txtBuscarDepartamentoSelector"
    ).value = "";

    mostrarDepartamentosSelector("");

    bootstrap.Dropdown.getOrCreateInstance(
        document.getElementById("btnDepartamentoSelector")
    ).hide();

    paginaSelector = 0;

    await cargarDispositivosSelector();
}

async function abrirSelectorDispositivos() {

    paginaSelector = 0;

    // Cargar las opciones de los filtros
    await cargarFiltrosSelector();

    // ==========================================
    // RESTAURAR LOS FILTROS COMO LOS DEJAMOS
    // ==========================================

    document.getElementById(
        "txtBuscarDispositivoTarea"
    ).value = buscarSelector;

    document.getElementById(
        "cmbRegistrosSelector"
    ).value = registrosSelector;

    document.getElementById(
        "cmbPlantaSelector"
    ).value = plantaSelector;

    document.getElementById(
        "cmbCategoriaSelector"
    ).value = categoriaSelector;


    // ==========================================
    // RESTAURAR DEPARTAMENTO
    // ==========================================

    if (departamentoSelector) {

        document.getElementById(
            "btnDepartamentoSelector"
        ).textContent = departamentoSelector;

    } else {

        document.getElementById(
            "btnDepartamentoSelector"
        ).textContent = "Todos los departamentos";
    }

    document.getElementById(
        "txtBuscarDepartamentoSelector"
    ).value = "";

    mostrarDepartamentosSelector("");


    // ==========================================
    // CARGAR DISPOSITIVOS CON LOS FILTROS
    // QUE YA ESTABAN SELECCIONADOS
    // ==========================================

    await cargarDispositivosSelector();

    modalDispositivos.show();
}

async function cargarDispositivosSelector() {

    const tbody =
        document.getElementById("tablaSeleccionDispositivos");

    tbody.innerHTML = `
        <tr>
            <td colspan="4" class="text-center py-4">
                Cargando dispositivos...
            </td>
        </tr>
    `;

    try {

        const url =
            `/devices/lista-paginada` +
            `?page=${paginaSelector}` +
            `&size=${registrosSelector}` +
            `&buscar=${encodeURIComponent(buscarSelector)}` +
            `&planta=${encodeURIComponent(plantaSelector)}` +
            `&departamento=${encodeURIComponent(departamentoSelector)}` +
            `&categoria=${encodeURIComponent(categoriaSelector)}`;

        const res = await fetch(url);

        if (!res.ok) {
            throw new Error("Error cargando dispositivos");
        }

        const pagina = await res.json();

        totalPaginasSelector = pagina.totalPages;

        const cantidadResultados =
            document.getElementById(
                "cantidadResultadosSelector"
            );

        if (cantidadResultados) {
            cantidadResultados.textContent =
                pagina.totalElements;
        }

        tbody.innerHTML = "";

        if (pagina.content.length === 0) {

            tbody.innerHTML = `
                <tr>
                    <td colspan="4"
                        class="text-center text-muted py-4">
                        No se encontraron dispositivos
                    </td>
                </tr>
            `;

        } else {

            pagina.content.forEach(t => {

                const seleccionado =
                    dispositivosSeleccionados.includes(t.id);

                tbody.innerHTML += `
                    <tr
                        class="fila-dispositivo ${seleccionado ? "seleccionado" : ""}"
                        data-id="${t.id}"
                        onclick="seleccionarFilaDispositivo(this)">

                        <td title="${t.activo ?? ""}">
                            ${t.activo ?? ""}
                        </td>

                        <td title="${t.device_name ?? ""}">
                            ${t.device_name ?? ""}
                        </td>

                        <td title="${t.empleado_asig ?? ""}">
                            ${t.empleado_asig ?? ""}
                        </td>

                        <td title="${t.planta ?? ""}">
                            ${t.planta ?? ""}
                        </td>

                        <td title="${t.area ?? ""}">
                            ${t.area ?? ""}
                        </td>

                        <td title="${t.departamento ?? ""}">
                            ${t.departamento ?? ""}
                        </td>

                        <td title="${t.categoria ?? ""}">
                            ${t.categoria ?? ""}
                        </td>

                    </tr>
                `;
            });
        }

        actualizarPaginacionSelector(
            pagina.number,
            pagina.totalPages,
            pagina.totalElements
        );

        // Si "Seleccionar todos" continúa marcado,
        // recalcular la selección usando los filtros actuales
        const chkTodos =
            document.getElementById(
                "chkSeleccionarTodosResultados"
            );

        if (chkTodos && chkTodos.checked) {

            await seleccionarTodosResultados(true);

        } else {

            actualizarCantidadSeleccionados();
        }

    } catch (error) {

        console.error(error);

        tbody.innerHTML = `
            <tr>
                <td colspan="4"
                    class="text-center text-danger py-4">
                    Error al cargar dispositivos
                </td>
            </tr>
        `;
    }
}



function seleccionarFilaDispositivo(fila) {

    const id = Number(fila.dataset.id);

    const indice = dispositivosSeleccionados.indexOf(id);

    if (indice === -1) {

        // No estaba seleccionado → seleccionar
        dispositivosSeleccionados.push(id);

        fila.classList.add("seleccionado");

    } else {

        // Ya estaba seleccionado → quitar selección
        dispositivosSeleccionados.splice(indice, 1);

        fila.classList.remove("seleccionado");
    }

    actualizarCantidadSeleccionados();
}


function guardarSeleccionPaginaActual() {

    document.querySelectorAll(
        "#tablaSeleccionDispositivos .chkDispositivo"
    ).forEach(check => {

        const id = Number(check.value);

        if (check.checked) {

            if (!dispositivosSeleccionados.includes(id)) {
                dispositivosSeleccionados.push(id);
            }

        } else {

            dispositivosSeleccionados =
                dispositivosSeleccionados.filter(
                    x => x !== id
                );
        }
    });

    actualizarCantidadSeleccionados();
}


function actualizarCantidadSeleccionados() {

    const cantidad = dispositivosSeleccionados.length;

    // Actualizar contador inferior
    const contador =
        document.getElementById(
            "cantidadDispositivosSeleccionados"
        );

    if (contador) {
        contador.textContent = cantidad;
    }


}


async function seleccionarTodosResultados(seleccionar) {

    const chkTodos =
        document.getElementById(
            "chkSeleccionarTodosResultados"
        );

    // Si desmarca "Seleccionar todos"
    if (!seleccionar) {

        dispositivosSeleccionados = [];

        document
            .querySelectorAll(
                "#tablaSeleccionDispositivos .fila-dispositivo"
            )
            .forEach(fila => {
                fila.classList.remove("seleccionado");
            });

        actualizarCantidadSeleccionados();

        return;
    }

    try {

        if (chkTodos) {
            chkTodos.disabled = true;
        }

        const url =
            `/devices/lista-paginada` +
            `?page=0` +
            `&size=100000` +
            `&buscar=${encodeURIComponent(buscarSelector)}` +
            `&planta=${encodeURIComponent(plantaSelector)}` +
            `&departamento=${encodeURIComponent(departamentoSelector)}` +
            `&categoria=${encodeURIComponent(categoriaSelector)}`;

        const res = await fetch(url);

        if (!res.ok) {
            throw new Error(
                "Error obteniendo dispositivos"
            );
        }

        const pagina = await res.json();


        // ==========================================
        // IMPORTANTE:
        // La selección pasa a ser EXACTAMENTE
        // los resultados del filtro actual
        // ==========================================

        dispositivosSeleccionados =
            pagina.content.map(t => t.id);


        // ==========================================
        // ACTUALIZAR LAS FILAS VISIBLES
        // ==========================================

        document
            .querySelectorAll(
                "#tablaSeleccionDispositivos .fila-dispositivo"
            )
            .forEach(fila => {

                const id =
                    Number(fila.dataset.id);

                if (
                    dispositivosSeleccionados.includes(id)
                ) {

                    fila.classList.add("seleccionado");

                } else {

                    fila.classList.remove("seleccionado");
                }

            });


        actualizarCantidadSeleccionados();

    } catch (error) {

        console.error(
            "Error seleccionando todos los resultados:",
            error
        );

        if (chkTodos) {
            chkTodos.checked = false;
        }

    } finally {

        if (chkTodos) {
            chkTodos.disabled = false;
        }
    }
}


function confirmarDispositivos() {

    guardarSeleccionPaginaActual();

    document.getElementById(
        "resumenDispositivos"
    ).textContent =
        dispositivosSeleccionados.length +
        " dispositivo(s) seleccionado(s)";

    modalDispositivos.hide();
}


function actualizarPaginacionSelector(
    pagina,
    totalPaginas,
    totalElementos
) {

    const inicio =
        totalElementos === 0
            ? 0
            : (pagina * registrosSelector) + 1;

    const fin = Math.min(
        (pagina + 1) * registrosSelector,
        totalElementos
    );

    document.getElementById(
        "infoPaginacionSelector"
    ).textContent =
        `Mostrando ${inicio}-${fin} de ${totalElementos}`;

    document.getElementById(
        "paginaActualSelector"
    ).textContent =
        `Página ${pagina + 1} de ${Math.max(totalPaginas, 1)}`;

    document.getElementById(
        "btnAnteriorSelector"
    ).disabled = pagina <= 0;

    document.getElementById(
        "btnSiguienteSelector"
    ).disabled =
        pagina >= totalPaginas - 1;
}


async function cambiarPaginaSelector(direccion) {

    guardarSeleccionPaginaActual();

    const nuevaPagina =
        paginaSelector + direccion;

    if (
        nuevaPagina < 0 ||
        nuevaPagina >= totalPaginasSelector
    ) {
        return;
    }

    paginaSelector = nuevaPagina;

    await cargarDispositivosSelector();
}


async function verDetalleTarea(id) {

    tareaDetalleId = id;

    paginaDetalle = 0;

    const res = await fetch(`/tareas/${id}/detalle`);

    const detalle = await res.json();

    document.getElementById("detalleTotal").textContent =
        detalle.totalDispositivos;

    document.getElementById("detalleCompletados").textContent =
        detalle.completados;

    document.getElementById("detallePendientes").textContent =
        detalle.pendientes;

    document.getElementById("detalleErrores").textContent =
        detalle.errores;

    const porcentaje = detalle.totalDispositivos === 0
        ? 0
        : Math.round((detalle.completados * 100) / detalle.totalDispositivos);

    const barra = document.getElementById("barraDetalle");

    barra.style.width = porcentaje + "%";

    barra.textContent =
        `${detalle.completados}/${detalle.totalDispositivos}`;

    document.getElementById("txtBuscarDetalle").value = "";

    document.getElementById("cmbEstadoDetalle").value = "";

    document.getElementById("cmbSizeDetalle").value = registrosDetalle;

    await cargarDispositivosDetalle();

    new bootstrap.Modal(
        document.getElementById("modalDetalleTarea")
    ).show();

}


let tareaDetalleId = 0;

let paginaDetalle = 0;

let totalPaginasDetalle = 0;

let registrosDetalle = 25;

async function cargarDispositivosDetalle() {

    const buscar =
        document.getElementById("txtBuscarDetalle").value;

    const estado =
        document.getElementById("cmbEstadoDetalle").value;

    const res = await fetch(

        `/tareas/${tareaDetalleId}/dispositivos?page=${paginaDetalle}&size=${registrosDetalle}&buscar=${buscar}&estado=${estado}`

    );

    const pagina = await res.json();

    totalPaginasDetalle = pagina.totalPages;
    document.getElementById("lblTotalDetalle").textContent =
        pagina.totalElements;

    document.getElementById("lblPaginaDetalle").textContent =
        `Página ${pagina.number + 1} de ${pagina.totalPages}`;

    const tbody =
        document.getElementById("tablaDetalleTarea");

    tbody.innerHTML = "";

    pagina.content.forEach(d => {

        let estadoTexto = "";
        let clase = "";

        switch (d.estado) {

            case "CONFIRMADA":

                estadoTexto = "✅ Confirmada";
                clase = "text-success";
                break;

            case "SIN_CONEXION":

                estadoTexto = "❌ Sin conexión";
                clase = "text-danger";
                break;

            case "PENDIENTE":

                estadoTexto = "⏳ Pendiente";
                clase = "text-warning";
                break;

            case "ENVIADA":

                estadoTexto = "📤 Enviada";
                clase = "text-primary";
                break;

        }

        tbody.innerHTML += `

            <tr>

                <td>${d.activo}</td>

                <td>${d.equipo}</td>

                <td class="${clase} fw-bold">

                    ${estadoTexto}

                </td>

                <td>${d.fechaEjecucion ?? "-"}</td>

            </tr>

        `;

    });

}


function paginaAnteriorDetalle() {

    if (paginaDetalle > 0) {

        paginaDetalle--;

        cargarDispositivosDetalle();

    }

}

function paginaSiguienteDetalle() {

    if (paginaDetalle < totalPaginasDetalle - 1) {

        paginaDetalle++;

        cargarDispositivosDetalle();

    }

}

function cambiarTamanoDetalle() {

    registrosDetalle =

        Number(document.getElementById("cmbSizeDetalle").value);

    paginaDetalle = 0;

    cargarDispositivosDetalle();

}


function cambiarTipoProgramacion() {

    const unaVez = document.getElementById("progUnaVez").checked;

    document.getElementById("panelUnaVez").style.display =
        unaVez ? "block" : "none";

    document.getElementById("panelRepetir").style.display =
        unaVez ? "none" : "block";

    if (!unaVez) {

        cambiarFrecuencia();

    }

}
function cambiarFrecuencia() {

    const frecuencia = document.getElementById("cmbFrecuencia").value;

    document.getElementById("configDiaria").style.display = "none";
    document.getElementById("configSemanal").style.display = "none";
    document.getElementById("configMensual").style.display = "none";

    switch (frecuencia) {

        case "DIARIA":

            document.getElementById("configDiaria").style.display = "block";
            break;

        case "SEMANAL":

            document.getElementById("configSemanal").style.display = "block";
            break;

        case "MENSUAL":

            document.getElementById("configMensual").style.display = "block";
            break;

    }

}


function cambiarTipoTarea() {

    const tipo = document.getElementById("cmbTipoTarea").value;

    const contenedor =
        document.getElementById("contenedorUrlApk");

    if (tipo === "ACTUALIZAR_APP") {

        contenedor.style.display = "block";

        document.getElementById("txtUrlApk").value =
            "https://github.com/Abner100MS/MonitoreoTablets/releases/download/V1.6/Monitoreo.apk";

    } else {

        contenedor.style.display = "none";

        document.getElementById("txtUrlApk").value = "";
    }
}

function filtrarDispositivosTarea() {

    const texto = document
        .getElementById("txtBuscarDispositivoTarea")
        .value
        .toLowerCase()
        .trim();

    const filas = document.querySelectorAll(
        "#tablaSeleccionDispositivos tr"
    );

    filas.forEach(fila => {

        const contenido = fila.textContent
            .toLowerCase();

        fila.style.display =
            contenido.includes(texto)
                ? ""
                : "none";
    });
}






document.addEventListener("input", function (e) {

    if (e.target.id !== "txtBuscarDispositivoTarea") {
        return;
    }

    clearTimeout(timeoutBusquedaSelector);

    timeoutBusquedaSelector = setTimeout(async () => {

        guardarSeleccionPaginaActual();

        buscarSelector = e.target.value.trim();

        paginaSelector = 0;

        await cargarDispositivosSelector();

    }, 300);
});


document.addEventListener("change", async function (e) {

    if (e.target.id === "chkSeleccionarTodosResultados") {

        await seleccionarTodosResultados(
            e.target.checked
        );

        return;
    }

    // =========================
    // FILTRO PLANTA
    // =========================

    if (e.target.id === "cmbPlantaSelector") {

        plantaSelector = e.target.value;
        paginaSelector = 0;

        await cargarDispositivosSelector();

        return;
    }


    // =========================
    // FILTRO CATEGORÍA
    // =========================

    if (e.target.id === "cmbCategoriaSelector") {

        categoriaSelector = e.target.value;

        paginaSelector = 0;

        await cargarDispositivosSelector();

        return;
    }


    // =========================
    // CANTIDAD DE REGISTROS
    // =========================

    if (e.target.id === "cmbRegistrosSelector") {

        guardarSeleccionPaginaActual();

        registrosSelector =
            Number(e.target.value);

        paginaSelector = 0;

        await cargarDispositivosSelector();

        return;
    }

    if (e.target.classList.contains("chkDispositivo")) {
        guardarSeleccionPaginaActual();
    }
});

document.addEventListener("input", function (e) {

    if (e.target.id === "txtBuscarDepartamentoSelector") {

        mostrarDepartamentosSelector(
            e.target.value
        );

    }

});