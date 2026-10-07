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


async function abrirModalEnviarStock(activo) {

    let modalElemento =
        document.getElementById("modalEnviarStockDispositivo");


    // =====================================================
    // SI stock.html TODAVÍA NO ESTÁ CARGADO
    // =====================================================

    if (!modalElemento) {

        const contenedor =
            document.getElementById("vista-stock");

        if (!contenedor) {
            console.error("No existe el contenedor vista-stock");
            return;
        }

        try {

            const response =
                await fetch("/modals/stock.html");

            if (!response.ok) {
                throw new Error("No se pudo cargar stock.html");
            }

            contenedor.innerHTML =
                await response.text();

            contenedor.dataset.cargado =
                "true";

            // Inicializar Stock
            if (typeof iniciarStock === "function") {
                iniciarStock();
            }

        } catch (error) {

            console.error(
                "Error cargando modal de Stock:",
                error
            );

            return;
        }


        // Ahora el modal ya debe existir
        modalElemento =
            document.getElementById(
                "modalEnviarStockDispositivo"
            );

    }


    if (!modalElemento) {
        console.error(
            "No se encontró modalEnviarStockDispositivo"
        );
        return;
    }


    // =====================================================
    // PREPARAR MODAL
    // =====================================================

    const inputActivo =
        document.getElementById(
            "dispositivoStockActivo"
        );

    const condicion =
        document.getElementById(
            "dispositivoStockCondicion"
        );

    const motivo =
        document.getElementById(
            "dispositivoStockMotivo"
        );

    const mensaje =
        document.getElementById(
            "mensajeEnviarStockDispositivo"
        );


    inputActivo.value = activo;

    condicion.value = "BUENO";

    motivo.value = "";

    mensaje.textContent = "";
    mensaje.style.display = "none";


    // =====================================================
    // ABRIR MODAL
    // =====================================================

    document.body.appendChild(modalElemento);

    // Abrir modal
    const modal =
        bootstrap.Modal.getOrCreateInstance(
            modalElemento
        );

    modal.show();
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


    // =====================================================
    // LIMPIAR MODAL LIBERAR AL CERRAR
    // =====================================================

    const modalLiberar =
        document.getElementById("modalLiberarStock");

    if (modalLiberar) {

        modalLiberar.addEventListener(
            "hidden.bs.modal",
            () => {

                activoPendienteLiberar = null;

                const mensaje =
                    document.getElementById("mensajeLiberarStock");

                if (mensaje) {
                    mensaje.style.display = "none";
                    mensaje.innerHTML = "";
                    mensaje.className = "stock-mensaje-liberar";
                }

                const activoTexto =
                    document.getElementById("activoLiberarStock");

                if (activoTexto) {
                    activoTexto.textContent = "---";
                }
            }
        );
    }
}

// =====================================================
// DATOS STOCK
// =====================================================

let dispositivosStock = [];
let activoPendienteEliminar = null;
let paginaStockActual = 1;
let registrosStockPorPagina = 10;

// =====================================================
// CARGAR STOCK DESDE BACKEND
// =====================================================

function eliminarStock(activo) {

    activoPendienteEliminar = activo;

    // Mostrar activo
    const activoTexto =
        document.getElementById("eliminarStockActivo");

    if (activoTexto) {
        activoTexto.textContent = activo;
    }

    // Limpiar motivo anterior
    const motivo =
        document.getElementById("eliminarStockMotivo");

    if (motivo) {
        motivo.value = "";
    }

    // Limpiar mensaje anterior
    const mensaje =
        document.getElementById("mensajeEliminarStock");

    if (mensaje) {
        mensaje.style.display = "none";
        mensaje.textContent = "";
    }

    // Abrir modal
    const modalElemento =
        document.getElementById("modalEliminarStock");

    if (!modalElemento) {
        console.error("No se encontró modalEliminarStock");
        return;
    }

    const modal =
        bootstrap.Modal.getOrCreateInstance(modalElemento);

    modal.show();
}

async function confirmarEliminarStock() {

    // =====================================================
    // VALIDAR ACTIVO
    // =====================================================

    if (!activoPendienteEliminar) {
        return;
    }

    // =====================================================
    // OBTENER MOTIVO
    // =====================================================

    const motivoInput =
        document.getElementById("eliminarStockMotivo");

    const motivo = motivoInput
        ? motivoInput.value.trim()
        : "";

    if (!motivo) {
        mostrarMensajeEliminarStock(
            "Debe indicar el motivo de la baja."
        );
        return;
    }

    // =====================================================
    // OBTENER USUARIO
    // =====================================================

    const usuario =
        sessionStorage.getItem("usuario");

    if (!usuario) {
        mostrarMensajeEliminarStock(
            "No se pudo identificar al usuario."
        );
        return;
    }

    // =====================================================
    // BOTÓN
    // =====================================================

    const boton =
        document.getElementById("btnConfirmarEliminarStock");

    if (boton) {
        boton.disabled = true;
        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-1"></span>
            Eliminando...
        `;
    }

    try {

        // =====================================================
        // DELETE
        // =====================================================

        const response = await fetch(
            `/api/stock/${encodeURIComponent(activoPendienteEliminar)}`,
            {
                method: "DELETE",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    motivo: motivo,
                    usuario: usuario
                })
            }
        );

        const data = await response.json();

        // =====================================================
        // ERROR DEL BACKEND
        // =====================================================

        if (!response.ok) {

            mostrarMensajeEliminarStock(
                data.message || "No se pudo dar de baja el equipo."
            );

            return;
        }

        // =====================================================
        // CERRAR MODAL
        // =====================================================

        const modalElemento =
            document.getElementById("modalEliminarStock");

        const modal =
            bootstrap.Modal.getInstance(modalElemento);

        if (modal) {
            modal.hide();
        }

        activoPendienteEliminar = null;

        // =====================================================
        // RECARGAR STOCK
        // =====================================================

        await cargarStock();

    } catch (error) {

        console.error(
            "Error eliminando equipo de Stock:",
            error
        );

        mostrarMensajeEliminarStock(
            "Ocurrió un error al intentar dar de baja el equipo."
        );

    } finally {

        // =====================================================
        // RESTAURAR BOTÓN
        // =====================================================

        if (boton) {

            boton.disabled = false;

            boton.innerHTML = `
                <i class="bi bi-trash3 me-1"></i>
                Dar de baja
            `;
        }
    }
}

function mostrarMensajeEliminarStock(mensaje) {

    const contenedor =
        document.getElementById("mensajeEliminarStock");

    if (!contenedor) {
        return;
    }

    contenedor.textContent = mensaje;
    contenedor.style.display = "block";
}


async function cargarStock() {

    try {

        const response = await fetch("/api/stock");

        if (!response.ok) {
            throw new Error(
                `Error al cargar Stock: ${response.status}`
            );
        }

        dispositivosStock = await response.json();

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
            <tr onclick="seleccionarFilaStock(this)">

                <td>
                    <span class="badge bg-primary">
                        ${item.activo || "---"}
                    </span>
                </td>


                <td>
                    ${item.equipo || "---"}
                </td>

                <td>
                    ${item.modelo || "---"}
                </td>

                <td>
                    ${item.codigo || "---"}
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
                    <div class="stock-motivo-actual"
                        title="${item.motivoIngreso || 'Sin motivo'}">
                        ${item.motivoIngreso || "---"}
                    </div>
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

                    <div class="d-flex justify-content-center gap-1">

                        <button
                            type="button"
                            class="btn btn-sm btn-outline-primary"
                            onclick="editarStock('${item.activo}')"
                            title="Editar información de Stock">

                            <i class="bi bi-pencil-square"></i>
                            Editar

                        </button>

                        <button
                            type="button"
                            class="btn btn-sm btn-outline-success"
                            onclick="liberarStock('${item.activo}')"
                            title="Liberar de Stock">

                            <i class="bi bi-box-arrow-up"></i>
                            Liberar

                        </button>

                        <button
                            type="button"
                            class="btn btn-sm btn-outline-danger"
                            onclick="eliminarStock('${item.activo}')"
                            title="Eliminar dispositivo">

                            <i class="bi bi-trash"></i>
                            Eliminar

                        </button>

                    </div>

                </td>

            </tr>
        `;

    }).join("");
    actualizarPaginacionStock(
        totalRegistros,
        totalPaginas
    );

}


function seleccionarFilaStock(fila) {

    // Si la fila ya está seleccionada, deseleccionarla
    if (fila.classList.contains("stock-fila-seleccionada")) {
        fila.classList.remove("stock-fila-seleccionada");
        return;
    }

    // Quitar selección de cualquier otra fila
    document
        .querySelectorAll(".stock-fila-seleccionada")
        .forEach(f => {
            f.classList.remove("stock-fila-seleccionada");
        });

    // Seleccionar la nueva fila
    fila.classList.add("stock-fila-seleccionada");
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
// LIBERAR DISPOSITIVO DE STOCK
// =====================================================

let activoPendienteLiberar = null;


function liberarStock(activo) {

    activoPendienteLiberar = activo;

    const activoTexto =
        document.getElementById("activoLiberarStock");

    if (activoTexto) {
        activoTexto.textContent = activo;
    }

    const modalElemento =
        document.getElementById("modalLiberarStock");

    const modal =
        bootstrap.Modal.getOrCreateInstance(modalElemento);

    modal.show();
}


async function confirmarLiberacionStock() {

    if (!activoPendienteLiberar) {
        return;
    }

    const boton =
        document.getElementById("btnConfirmarLiberarStock");

    const textoOriginal = boton.innerHTML;

    try {

        boton.disabled = true;

        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-1"></span>
            Liberando...
        `;

        const usuario = sessionStorage.getItem("usuario");

        const response = await fetch(
            `/api/stock/${encodeURIComponent(activoPendienteLiberar)}/liberar`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    comentario: "Equipo liberado de Stock",
                    usuario: usuario
                })
            }
        );

        const resultado = await response.json();


        // ERROR
        if (!response.ok) {

            mostrarMensajeLiberarStock(
                resultado.message ||
                "No fue posible liberar el dispositivo.",
                "error"
            );

            return;
        }


        // CERRAR MODAL
        const modalElemento =
            document.getElementById("modalLiberarStock");

        const modal =
            bootstrap.Modal.getInstance(modalElemento);

        if (modal) {
            modal.hide();
        }


        activoPendienteLiberar = null;

        await cargarStock();

    } catch (error) {

        console.error(
            "STOCK | Error liberando dispositivo:",
            error
        );

        mostrarMensajeLiberarStock(
            "Ocurrió un error al liberar el dispositivo.",
            "error"
        );

    } finally {

        boton.disabled = false;
        boton.innerHTML = textoOriginal;
    }
}



// =====================================================
// MENSAJE MODAL LIBERAR STOCK
// =====================================================

function mostrarMensajeLiberarStock(mensaje, tipo = "error") {

    const contenedor =
        document.getElementById("mensajeLiberarStock");

    if (!contenedor) {
        return;
    }

    contenedor.className =
        `stock-mensaje-liberar ${tipo}`;

    contenedor.innerHTML = `
        <i class="bi bi-exclamation-triangle-fill"></i>

        <span>
            ${mensaje}
        </span>
    `;

    contenedor.style.display = "flex";
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

        // ==========================================
        // BUSCADOR
        // ==========================================

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


        // ==========================================
        // FILTRO
        // ==========================================

        let coincideCondicion = true;

        if (condicion === "REPORTANDO") {

            coincideCondicion =
                item.reportando === true;

        } else if (condicion) {

            coincideCondicion =
                item.condicion === condicion;
        }


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

    const totalReportando =
        lista.filter(item =>
            item.reportando === true
        ).length;


    const elementoTotal =
        document.getElementById("stockTotal");

    const elementoFalla =
        document.getElementById("stockFalla");

    const elementoReportando =
        document.getElementById("stockReportando");


    if (elementoTotal) {
        elementoTotal.textContent = total;
    }

    if (elementoFalla) {
        elementoFalla.textContent = fallas;
    }

    if (elementoReportando) {
        elementoReportando.textContent = totalReportando;
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
// ABRIR MODAL EDITAR STOCK
// =====================================================

function editarStock(activo) {

    const item = dispositivosStock.find(
        d => String(d.activo) === String(activo)
    );

    if (!item) {
        console.error(
            "STOCK | No se encontró el activo:",
            activo
        );
        return;
    }

    // ACTIVO
    const activoTexto =
        document.getElementById("editarStockActivo");

    if (activoTexto) {
        activoTexto.textContent = item.activo;
    }


    // CONDICIÓN ACTUAL
    const condicion =
        document.getElementById("editarStockCondicion");

    if (condicion) {
        condicion.value = item.condicion || "BUENO";
    }


    const motivo =
        document.getElementById("editarStockMotivo");

    if (motivo) {
        motivo.value = "";
    }

    // LIMPIAR MENSAJE ANTERIOR
    const mensaje =
        document.getElementById("mensajeEditarStock");

    if (mensaje) {
        mensaje.style.display = "none";
        mensaje.innerHTML = "";
        mensaje.className = "stock-mensaje-editar";
    }


    // GUARDAR ACTIVO EN EL BOTÓN
    const boton =
        document.getElementById("btnGuardarEditarStock");

    if (boton) {
        boton.dataset.activo = activo;
    }


    // ABRIR MODAL
    const modalElemento =
        document.getElementById("modalEditarStock");

    const modal =
        bootstrap.Modal.getOrCreateInstance(modalElemento);

    modal.show();
}


async function guardarEdicionStock() {

    const boton =
        document.getElementById("btnGuardarEditarStock");

    const activo = boton.dataset.activo;

    const condicion =
        document.getElementById("editarStockCondicion").value;

    const motivo =
        document.getElementById("editarStockMotivo").value.trim();

    const usuario =
        sessionStorage.getItem("usuario");

    // =====================================================
    // VALIDACIONES
    // =====================================================

    if (!motivo) {
        mostrarMensajeEditarStock(
            "Debe indicar el motivo del cambio."
        );

        document.getElementById("editarStockMotivo").focus();
        return;
    }

    if (!usuario) {
        mostrarMensajeEditarStock(
            "No se pudo identificar al usuario que realiza el cambio."
        );
        return;
    }

    const textoOriginal = boton.innerHTML;

    try {

        boton.disabled = true;

        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-1"></span>
            Guardando...
        `;

        const response = await fetch(
            `/api/stock/${encodeURIComponent(activo)}`,
            {
                method: "PUT",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    condicion: condicion,
                    motivo: motivo,
                    usuario: usuario
                })
            }
        );

        const resultado = await response.json();

        if (!response.ok) {

            mostrarMensajeEditarStock(
                resultado.message ||
                "No fue posible actualizar el equipo."
            );

            return;
        }

        const modalElemento =
            document.getElementById("modalEditarStock");

        const modal =
            bootstrap.Modal.getInstance(modalElemento);

        if (modal) {
            modal.hide();
        }

        await cargarStock();

    } catch (error) {

        console.error(
            "STOCK | Error actualizando dispositivo:",
            error
        );

        mostrarMensajeEditarStock(
            "Ocurrió un error al actualizar el equipo."
        );

    } finally {

        boton.disabled = false;
        boton.innerHTML = textoOriginal;

    }
}

function mostrarMensajeEditarStock(mensaje) {

    const contenedor =
        document.getElementById("mensajeEditarStock");

    if (!contenedor) return;

    contenedor.className =
        "stock-mensaje-editar error";

    contenedor.innerHTML = `
        <i class="bi bi-exclamation-triangle-fill"></i>
        <span>${mensaje}</span>
    `;

    contenedor.style.display = "flex";
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

        const usuario = sessionStorage.getItem("usuario");

        const response = await fetch("/api/stock", {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                activo: activo,
                motivo: motivo,
                condicion: condicion,
                observacion: observacion,
                usuario: usuario
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



async function confirmarEnviarDispositivoStock() {

    const activo =
        document.getElementById("dispositivoStockActivo").value.trim();

    const condicion =
        document.getElementById("dispositivoStockCondicion").value;

    const motivo =
        document.getElementById("dispositivoStockMotivo").value.trim();

    const usuario =
        sessionStorage.getItem("usuario");

    const mensaje =
        document.getElementById("mensajeEnviarStockDispositivo");

    const boton =
        document.getElementById("btnEnviarDispositivoStock");


    // ==============================
    // VALIDACIONES
    // ==============================

    if (!motivo) {
        mensaje.textContent = "Debe indicar el motivo de ingreso.";
        mensaje.style.display = "block";
        return;
    }

    if (!usuario) {
        mensaje.textContent = "No se pudo identificar al usuario.";
        mensaje.style.display = "block";
        return;
    }


    // ==============================
    // ENVIAR A STOCK
    // ==============================

    try {

        boton.disabled = true;

        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-1"></span>
            Enviando...
        `;


        const response = await fetch("/api/stock", {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                activo: activo,
                motivo: motivo,
                condicion: condicion,
                observacion: "",
                usuario: usuario
            })

        });


        const data = await response.json();


        if (!response.ok) {

            mensaje.textContent =
                data.message || "No se pudo enviar el equipo a Stock.";

            mensaje.style.display = "block";

            return;
        }


        // ==============================
        // CERRAR MODAL
        // ==============================

        const modalElemento =
            document.getElementById("modalEnviarStockDispositivo");

        const modal =
            bootstrap.Modal.getInstance(modalElemento);

        if (modal) {
            modal.hide();
        }


        // Actualizar Stock si ya está cargado
        if (typeof cargarStock === "function") {
            await cargarStock();
        }


    } catch (error) {

        console.error(
            "Error enviando dispositivo a Stock:",
            error
        );

        mensaje.textContent =
            "Ocurrió un error al enviar el equipo a Stock.";

        mensaje.style.display = "block";


    } finally {

        boton.disabled = false;

        boton.innerHTML = `
            <i class="bi bi-box-arrow-in-down me-1"></i>
            Enviar a Stock
        `;

    }

}

function abrirReporteStock() {

    const modalElemento =
        document.getElementById("modalReporteStock");

    if (!modalElemento) {
        console.error("No se encontró modalReporteStock");
        return;
    }

    // Mover el modal directamente al body
    // para evitar que quede detrás de la vista Stock
    document.body.appendChild(modalElemento);

    const modal =
        bootstrap.Modal.getOrCreateInstance(
            modalElemento
        );

    modal.show();
}


function generarReporteExcelStock() {

    // =====================================================
    // FILTROS ACTUALES DE STOCK
    // =====================================================

    const buscar =
        document.getElementById("buscarStock")
            ?.value
            .trim() || "";

    const filtro =
        document.getElementById("filtroCondicionStock")
            ?.value || "";


    // =====================================================
    // COLUMNAS SELECCIONADAS
    // =====================================================

    const columnas = [];

    document
        .querySelectorAll(".stock-reporte-columna:checked")
        .forEach(c => {

            columnas.push(c.value);

        });


    // =====================================================
    // VALIDAR COLUMNAS
    // =====================================================

    if (columnas.length === 0) {

        Swal.fire({
            icon: "warning",
            title: "Seleccione columnas",
            text: "Debe seleccionar al menos una columna para generar el reporte."
        });

        return;
    }


    // =====================================================
    // CONSTRUIR URL
    // =====================================================

    const url =
        `/api/stock/reporte?` +
        `buscar=${encodeURIComponent(buscar)}` +
        `&filtro=${encodeURIComponent(filtro)}` +
        `&columnas=${encodeURIComponent(columnas.join(","))}`;


    console.log(
        "STOCK | Generando reporte:",
        url
    );


    // =====================================================
    // DESCARGAR EXCEL
    // =====================================================

    const a =
        document.createElement("a");

    a.href = url;

    a.style.display = "none";


    document.body.appendChild(a);

    a.click();

    document.body.removeChild(a);


    // =====================================================
    // CERRAR MODAL
    // =====================================================

    const modalElemento =
        document.getElementById("modalReporteStock");

    const modal =
        bootstrap.Modal.getInstance(
            modalElemento
        );

    if (modal) {

        modal.hide();
    }
}