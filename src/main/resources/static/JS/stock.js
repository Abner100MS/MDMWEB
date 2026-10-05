// =====================================================
// VISTA STOCK
// =====================================================

async function mostrarVistaStock() {

    guardarVista("stock");

    vistaActual = "stock";

    detenerTrackingSiExiste();

    ocultarTodasLasVistas();

    document.getElementById("btnActualizar").style.display = "inline-block";

    document.getElementById("vista-stock").style.display = "block";


    // ==========================================
    // MENÚ LATERAL
    // ==========================================

    document.querySelectorAll(".sidebar .nav-link").forEach(x => {

        x.classList.remove(
            "active",
            "text-white"
        );

        x.classList.add(
            "text-secondary"
        );

    });


    document.getElementById("menuStock")
        .classList.remove("text-secondary");

    document.getElementById("menuStock")
        .classList.add(
            "active",
            "text-white"
        );


    // ==========================================
    // CARGAR VISTA STOCK
    // ==========================================

    const contenedor =
        document.getElementById("vista-stock");


    if (!contenedor.dataset.cargado) {

        const res =
            await fetch("/modals/stock.html");

        contenedor.innerHTML =
            await res.text();


        // Inicializar eventos de Stock
        iniciarStock();


        // Cargar dispositivos en Stock
        cargarStock();


        contenedor.dataset.cargado =
            "true";

    } else {

        cargarStock();

    }

}


// =====================================================
// INICIALIZAR STOCK
// =====================================================
function iniciarStock() {

    const buscar =
        document.getElementById("buscarStock");

    const condicion =
        document.getElementById("filtroCondicionStock");


    // BUSCADOR DE LA TABLA STOCK
    if (buscar) {

        buscar.addEventListener("input", () => {
            paginaStockActual = 1;
            filtrarStock();
        });
    }


    // FILTRO DE CONDICIÓN
    if (condicion) {

        condicion.addEventListener("change", () => {
            paginaStockActual = 1;
            filtrarStock();
        });
    }


    // ENTER EN BUSCADOR DEL MODAL
    const inputActivoStock =
        document.getElementById("stockActivoBuscar");

    if (inputActivoStock) {

        inputActivoStock.addEventListener(
            "keydown",
            function (event) {

                if (event.key === "Enter") {

                    event.preventDefault();

                    buscarActivoParaStock();
                }
            }
        );
    }


    // LIMPIAR MODAL CUANDO SE CIERRE
    const modalIngresoStock =
        document.getElementById("modalIngresoStock");

    if (modalIngresoStock) {

        modalIngresoStock.addEventListener(
            "hidden.bs.modal",
            function () {

                limpiarIngresoStock();

            }
        );
    }
}

// =====================================================
// DATOS STOCK
// =====================================================

let dispositivosStock = [];

let paginaStockActual = 1;
let registrosStockPorPagina = 10;

// =====================================================
// CARGAR STOCK DESDE BACKEND
// =====================================================

async function cargarStock() {

    try {

        const response = await fetch("/api/stock");

        if (!response.ok) {
            throw new Error(
                `Error al cargar Stock: ${response.status}`
            );
        }

        dispositivosStock = await response.json();

        console.log(
            "STOCK | Dispositivos recibidos:",
            dispositivosStock
        );

        renderizarStock(dispositivosStock);

        actualizarResumenStock(dispositivosStock);

    } catch (error) {

        console.error(
            "Error cargando dispositivos de Stock:",
            error
        );

    }
}


// =====================================================
// RENDERIZAR TABLA
// =====================================================

function renderizarStock(lista) {

    const totalRegistros = lista.length;

    const totalPaginas =
        Math.max(
            1,
            Math.ceil(totalRegistros / registrosStockPorPagina)
        );

    if (paginaStockActual > totalPaginas) {
        paginaStockActual = totalPaginas;
    }

    const inicio =
        (paginaStockActual - 1) * registrosStockPorPagina;

    const fin =
        inicio + registrosStockPorPagina;

    const listaPagina =
        lista.slice(inicio, fin);

    const tbody =
        document.getElementById("tablaStockBody");

    if (!tbody) {
        return;
    }


    // SIN DISPOSITIVOS
    if (!lista || lista.length === 0) {

        tbody.innerHTML = `
            <tr>
                <td colspan="8"
                    class="text-center py-5 text-muted">

                    <i class="bi bi-box-seam fs-1 d-block mb-2"></i>

                    No hay dispositivos en Stock

                </td>
            </tr>
        `;

        return;
    }


    // CON DISPOSITIVOS
    tbody.innerHTML = listaPagina.map(item => {

        const fecha =
            item.fechaIngreso
                ? formatearFechaStock(item.fechaIngreso)
                : "---";


        const condicion =
            item.condicion || "---";


        return `
            <tr>

                <td>
                    <span class="badge bg-primary">
                        ${item.activo || "---"}
                    </span>
                </td>


                <td>
                    ${item.modelo || "---"}
                </td>


                <td>
                    ${item.empleado || "---"}
                </td>


                <td>
                    <div>
                        ${item.area || "---"}
                    </div>

                    <small class="text-muted">
                        ${item.departamento || "---"}
                    </small>
                </td>


                <td>
                    ${crearBadgeCondicionStock(condicion)}
                </td>


                <td>
                    ${fecha}
                </td>


                <td>
                    <span class="badge bg-secondary">
                        EN STOCK
                    </span>
                </td>


                <td class="text-center">

                    <button
                        type="button"
                        class="btn btn-sm btn-outline-primary"
                        onclick="gestionarStock('${item.activo}')">

                        <i class="bi bi-gear"></i>
                        Gestionar

                    </button>

                </td>

            </tr>
        `;

    }).join("");
    actualizarPaginacionStock(
        totalRegistros,
        totalPaginas
    );

}


// =====================================================
// ACTUALIZAR PAGINACIÓN STOCK
// =====================================================

function actualizarPaginacionStock(
    totalRegistros,
    totalPaginas
) {

    const info =
        document.getElementById("stockInfoPagina");

    const total =
        document.getElementById("stockTotalRegistros");

    const anterior =
        document.getElementById("btnStockAnterior");

    const siguiente =
        document.getElementById("btnStockSiguiente");


    if (info) {
        info.textContent =
            `Página ${paginaStockActual} de ${totalPaginas}`;
    }

    if (total) {
        total.textContent = totalRegistros;
    }

    if (anterior) {
        anterior.disabled =
            paginaStockActual <= 1;
    }

    if (siguiente) {
        siguiente.disabled =
            paginaStockActual >= totalPaginas;
    }
}

// =====================================================
// CAMBIAR PÁGINA STOCK
// =====================================================

function cambiarPaginaStock(direccion) {

    paginaStockActual += direccion;

    filtrarStock();
}

// =====================================================
// CAMBIAR CANTIDAD DE REGISTROS POR PÁGINA
// =====================================================

function cambiarCantidadStock() {

    const select =
        document.getElementById("stockRegistrosPorPagina");

    registrosStockPorPagina =
        parseInt(select.value, 10);

    // Regresar siempre a la primera página
    paginaStockActual = 1;

    filtrarStock();
}


// =====================================================
// BADGE CONDICIÓN
// =====================================================

function crearBadgeCondicionStock(condicion) {

    switch (condicion) {

        case "BUENO":
            return `
                <span class="badge bg-success">
                    BUENO
                </span>
            `;

        case "CON FALLA":
            return `
                <span class="badge bg-danger">
                    CON FALLA
                </span>
            `;

        case "EN REPARACION":
            return `
                <span class="badge bg-warning text-dark">
                    EN REPARACIÓN
                </span>
            `;

        default:
            return `
                <span class="badge bg-secondary">
                    ${condicion}
                </span>
            `;
    }

}


// =====================================================
// FILTRAR STOCK
// =====================================================

function filtrarStock() {

    const texto =
        document.getElementById("buscarStock")
            ?.value
            .trim()
            .toLowerCase() || "";


    const condicion =
        document.getElementById("filtroCondicionStock")
            ?.value || "";


    const filtrados = dispositivosStock.filter(item => {

        const coincideTexto =

            !texto ||

            String(item.activo || "")
                .toLowerCase()
                .includes(texto) ||

            String(item.modelo || "")
                .toLowerCase()
                .includes(texto) ||

            String(item.empleado || "")
                .toLowerCase()
                .includes(texto) ||

            String(item.area || "")
                .toLowerCase()
                .includes(texto);


        const coincideCondicion =

            !condicion ||

            item.condicion === condicion;


        return coincideTexto && coincideCondicion;

    });


    renderizarStock(filtrados);

}


// =====================================================
// RESUMEN SUPERIOR
// =====================================================

function actualizarResumenStock(lista) {

    const total =
        lista.length;


    const fallas =
        lista.filter(item =>
            item.condicion === "CON FALLA"
        ).length;


    const elementoTotal =
        document.getElementById("stockTotal");

    const elementoFalla =
        document.getElementById("stockFalla");


    if (elementoTotal) {
        elementoTotal.textContent = total;
    }


    if (elementoFalla) {
        elementoFalla.textContent = fallas;
    }


    // Esto lo conectaremos después con el estado
    // real de dispositivos que siguen reportando.
    const reportando =
        document.getElementById("stockReportando");

    if (reportando) {
        reportando.textContent = "0";
    }

}


// =====================================================
// FORMATEAR FECHA
// =====================================================

function formatearFechaStock(fecha) {

    const date = new Date(fecha);

    if (isNaN(date.getTime())) {
        return fecha;
    }

    return date.toLocaleString("es-GT", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });

}


// =====================================================
// GESTIONAR
// =====================================================

function gestionarStock(activo) {

    console.log(
        "Gestionar dispositivo Stock:",
        activo
    );

}


// =====================================================
// LIMPIAR MODAL INGRESO STOCK
// =====================================================

function limpiarIngresoStock() {

    // BUSCADOR
    document.getElementById("stockActivoBuscar").value = "";


    // INFORMACIÓN DEL ACTIVO
    document.getElementById("stockInfoActivo").textContent = "---";
    document.getElementById("stockInfoEquipo").textContent = "---";
    document.getElementById("stockInfoModelo").textContent = "---";
    document.getElementById("stockInfoCodigo").textContent = "---";
    document.getElementById("stockInfoEmpleado").textContent = "---";
    document.getElementById("stockInfoPlanta").textContent = "---";
    document.getElementById("stockInfoArea").textContent = "---";
    document.getElementById("stockInfoDepartamento").textContent = "---";


    // DEJAR INFORMACIÓN EN ESTADO PENDIENTE
    const informacion =
        document.getElementById("stockInformacionActivo");

    informacion.classList.add("stock-info-pendiente");


    // CONDICIÓN
    const condicion =
        document.getElementById("stockCondicionIngreso");

    condicion.value = "BUENO";
    condicion.disabled = true;


    // MOTIVO
    const motivo =
        document.getElementById("stockMotivoIngreso");

    motivo.value = "";
    motivo.disabled = true;


    // OBSERVACIÓN
    const observacion =
        document.getElementById("stockObservacionIngreso");

    observacion.value = "";
    observacion.disabled = true;

    observacion.style.height = "";
    observacion.style.width = "";


    // MENSAJE
    const mensaje =
        document.getElementById("stockMensajeBusqueda");

    mensaje.textContent =
        "Ingrese un número de activo para consultar su información.";

    mensaje.className =
        "stock-mensaje-busqueda";


    // BOTÓN INGRESAR
    document.getElementById(
        "btnConfirmarIngresoStock"
    ).disabled = true;
}


function abrirIngresoStock() {

    // Siempre abrir limpio
    limpiarIngresoStock();


    const modal =
        bootstrap.Modal.getOrCreateInstance(
            document.getElementById("modalIngresoStock")
        );

    modal.show();


    setTimeout(() => {

        document
            .getElementById("stockActivoBuscar")
            .focus();

    }, 300);
}

// =====================================================
// BUSCAR ACTIVO PARA INGRESAR A STOCK
// =====================================================

async function buscarActivoParaStock() {

    const inputActivo = document.getElementById("stockActivoBuscar");
    const activo = inputActivo.value.trim();

    const mensaje = document.getElementById("stockMensajeBusqueda");
    const informacion = document.getElementById("stockInformacionActivo");
    const botonGuardar = document.getElementById("btnConfirmarIngresoStock");


    // ==========================================
    // VALIDAR ACTIVO
    // ==========================================

    if (!activo) {

        mensaje.textContent = "Ingrese un número de activo.";
        mensaje.className = "text-danger";

        botonGuardar.disabled = true;

        return;
    }


    try {

        // ==========================================
        // BUSCAR EN ACTIVO_INFO
        // ==========================================

        const response = await fetch(
            `/api/stock/buscar/${encodeURIComponent(activo)}`
        );


        // ==========================================
        // NO ENCONTRADO
        // ==========================================

        if (!response.ok) {

            const error = await response.json().catch(() => ({}));

            mensaje.textContent =
                error.message || "No se encontró el activo.";

            mensaje.className =
                "stock-mensaje-busqueda text-danger";


            // LIMPIAR INFORMACIÓN ANTERIOR
            document.getElementById("stockInfoActivo").textContent = "---";
            document.getElementById("stockInfoEquipo").textContent = "---";
            document.getElementById("stockInfoModelo").textContent = "---";
            document.getElementById("stockInfoCodigo").textContent = "---";
            document.getElementById("stockInfoEmpleado").textContent = "---";
            document.getElementById("stockInfoPlanta").textContent = "---";
            document.getElementById("stockInfoArea").textContent = "---";
            document.getElementById("stockInfoDepartamento").textContent = "---";


            // DEJAR INFORMACIÓN EN ESTADO PENDIENTE
            informacion.classList.add("stock-info-pendiente");


            // BLOQUEAR FORMULARIO
            document.getElementById("stockCondicionIngreso").disabled = true;
            document.getElementById("stockMotivoIngreso").disabled = true;
            document.getElementById("stockObservacionIngreso").disabled = true;

            botonGuardar.disabled = true;

            return;
        }


        // ==========================================
        // ACTIVO ENCONTRADO
        // ==========================================

        const datos = await response.json();

        console.log(
            "STOCK | Información activo:",
            datos
        );


        // ==========================================
        // LLENAR INFORMACIÓN
        // ==========================================

        document.getElementById("stockInfoActivo").textContent =
            datos.activo || "---";

        document.getElementById("stockInfoCodigo").textContent =
            datos.codigo || "---";

        document.getElementById("stockInfoEmpleado").textContent =
            datos.empleado || "---";

        document.getElementById("stockInfoPlanta").textContent =
            datos.planta || "---";

        document.getElementById("stockInfoArea").textContent =
            datos.area || "---";

        document.getElementById("stockInfoDepartamento").textContent =
            datos.departamento || "---";

        document.getElementById("stockInfoEquipo").textContent =
            datos.equipo || "---";

        document.getElementById("stockInfoModelo").textContent =
            datos.modelo || "---";


        // ==========================================
        // MOSTRAR DATOS
        // ==========================================

        informacion.classList.remove("stock-info-pendiente");

        document.getElementById("stockCondicionIngreso").disabled = false;
        document.getElementById("stockMotivoIngreso").disabled = false;
        document.getElementById("stockObservacionIngreso").disabled = false;

        botonGuardar.disabled = false;

        mensaje.textContent =
            "Activo encontrado correctamente.";

        mensaje.className =
            "stock-mensaje-busqueda stock-mensaje-ok";


    } catch (error) {

        console.error(
            "STOCK | Error buscando activo:",
            error
        );

        mensaje.textContent =
            "Error al consultar la información del activo.";

        mensaje.className = "text-danger";

        informacion.classList.add("stock-info-pendiente");
        botonGuardar.disabled = true;
    }
}


// =====================================================
// GUARDAR INGRESO A STOCK
// =====================================================

async function guardarIngresoStock() {

    const activo =
        document.getElementById("stockInfoActivo").textContent.trim();

    const condicion =
        document.getElementById("stockCondicionIngreso").value;

    const motivo =
        document.getElementById("stockMotivoIngreso").value.trim();

    const observacion =
        document.getElementById("stockObservacionIngreso").value.trim();

    const boton =
        document.getElementById("btnConfirmarIngresoStock");


    // =====================================================
    // VALIDACIONES
    // =====================================================

    if (!activo || activo === "---") {

        Swal.fire({
            icon: "warning",
            title: "Activo requerido",
            text: "Primero debe buscar y seleccionar un activo."
        });

        return;
    }


    if (!motivo) {

        Swal.fire({
            icon: "warning",
            title: "Motivo requerido",
            text: "Ingrese el motivo por el cual el equipo entra a Stock."
        });

        document
            .getElementById("stockMotivoIngreso")
            .focus();

        return;
    }


    try {

        // Evitar doble clic
        boton.disabled = true;

        const textoOriginal = boton.innerHTML;

        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-2"></span>
            Ingresando...
        `;


        // =====================================================
        // POST STOCK
        // =====================================================

        const response = await fetch("/api/stock", {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                activo: activo,
                motivo: motivo,
                condicion: condicion,
                observacion: observacion
            })
        });


        const resultado =
            await response.json().catch(() => ({}));


        // =====================================================
        // ERROR DEL BACKEND
        // =====================================================

        if (!response.ok) {

            boton.disabled = false;
            boton.innerHTML = textoOriginal;

            Swal.fire({
                icon: "warning",
                title: "No se pudo ingresar",
                text:
                    resultado.message ||
                    "No fue posible ingresar el dispositivo a Stock."
            });

            return;
        }


        // =====================================================
        // CERRAR MODAL
        // =====================================================

        const elementoModal =
            document.getElementById("modalIngresoStock");

        const modal =
            bootstrap.Modal.getInstance(elementoModal);

        if (modal) {
            modal.hide();
        }


        // =====================================================
        // ACTUALIZAR TABLA STOCK
        // =====================================================

        await cargarStock();


        // =====================================================
        // MENSAJE
        // =====================================================

        Swal.fire({
            icon: "success",
            title: "Ingresado a Stock",
            text:
                resultado.message ||
                `El activo ${activo} fue ingresado a Stock correctamente.`,
            timer: 2000,
            showConfirmButton: false
        });


    } catch (error) {

        console.error(
            "STOCK | Error ingresando dispositivo:",
            error
        );

        boton.disabled = false;

        Swal.fire({
            icon: "error",
            title: "Error",
            text: "Ocurrió un error al ingresar el dispositivo a Stock."
        });
    }
}