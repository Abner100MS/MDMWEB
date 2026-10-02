let detalleTabletId = null;
let detalleTabletActivo = null;

document.addEventListener("DOMContentLoaded", async () => {

    try {

        const response = await fetch("/modals/detalle.html");

        if (!response.ok) {
            throw new Error("No se pudo cargar detalle.html");
        }

        const html = await response.text();

        const contenedor =
            document.getElementById("contenedorModalDetalle");

        if (!contenedor) {
            console.error("No existe #contenedorModalDetalle");
            return;
        }

        contenedor.innerHTML = html;

    } catch (error) {

        console.error(
            "Error cargando modal Detalles:",
            error
        );

    }

});


function cambiarEstadoBateriaDetalle() {

    const estado =
        document.getElementById("detalleEstadoBateria").value;

    const porcentaje =
        document.getElementById("detallePorcentajeInflado");

    if (estado === "INFLADA") {

        porcentaje.disabled = false;

    } else {

        porcentaje.value = "";
        porcentaje.disabled = true;

    }
}


async function abrirModalDetalle(id, activo) {

    detalleTabletId = id;
    detalleTabletActivo = activo;


    const diagnosticoWebSocket =
        document.getElementById("diagnosticoWebSocket");

    if (diagnosticoWebSocket) {

        diagnosticoWebSocket.innerHTML =
            `<span class="text-muted">Sin comprobar</span>`;

        document.getElementById("diagnosticoComando").innerHTML =
            `<span class="text-muted">Sin comprobar</span>`;

        document.getElementById("diagnosticoAck").innerHTML =
            `<span class="text-muted">Sin comprobar</span>`;

        document.getElementById("diagnosticoLatencia").textContent = "--";

        const resultado =
            document.getElementById("diagnosticoResultado");

        resultado.className =
            "alert alert-secondary d-flex align-items-center mt-3 mb-3";

        document.getElementById("diagnosticoResultadoIcono").className =
            "bi bi-info-circle-fill me-3";

        document.getElementById("diagnosticoResultadoTitulo").textContent =
            "Diagnóstico no ejecutado";

        document.getElementById("diagnosticoResultadoMensaje").textContent =
            "Ejecuta la prueba para verificar la comunicación remota.";
    }

    // Mostrar activo en el encabezado
    document.getElementById("detalle-activo-label").textContent = activo;

    // Limpiar temporalmente mientras consulta
    document.getElementById("detalleEstadoBateria").value = "NORMAL";
    document.getElementById("detallePorcentajeInflado").value = "";
    cambiarEstadoBateriaDetalle();

    try {

        const response = await fetch(`/devices/activo/${activo}`);

        if (!response.ok) {
            throw new Error("No se pudo obtener la información del dispositivo");
        }

        const tablet = await response.json();

        // console.log("Detalle dispositivo:", tablet);

        // ==========================
        // INFORMACIÓN DEL ACTIVO
        // ==========================

        let activoInfo = null;

        try {

            const responseActivo =
                await fetch(`/devices/activo-info/${activo}`);

            if (responseActivo.ok && responseActivo.status !== 204) {
                activoInfo = await responseActivo.json();
            }

        } catch (error) {

            console.warn(
                "No se pudo obtener información adicional del activo:",
                error
            );
        }

        //console.log("Información activo:", activoInfo);


        cargarInformacionDispositivo(tablet, activoInfo);

        // ==========================
        // CARGAR BATERÍA
        // ==========================

        const estado = tablet.estado_bateria || "NORMAL";

        document.getElementById("detalleEstadoBateria").value = estado;

        if (
            estado === "INFLADA" &&
            tablet.porcentaje_inflado !== null &&
            tablet.porcentaje_inflado !== undefined
        ) {
            document.getElementById("detallePorcentajeInflado").value =
                tablet.porcentaje_inflado;
        }

        cambiarEstadoBateriaDetalle();

        await cargarAuditoriaBateria(activo);

        // Abrir modal
        const modalElement =
            document.getElementById("modalDetalle");

        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

        modal.show();

    } catch (error) {

        console.error("Error cargando detalles:", error);

        mostrarToastDetalle(
            "error",
            "Error al cargar",
            "No se pudieron cargar los detalles del dispositivo."
        );

    }
}

function cargarInformacionDispositivo(tablet, activoInfo) {

    // ==========================================
    // INFORMACIÓN GENERAL
    // ==========================================

    document.getElementById("detalleActivo").textContent =
        tablet.activo || "--";

    document.getElementById("detalleNombreEquipo").textContent =
        tablet.device_name || "--";

    document.getElementById("detalleModelo").textContent =
        tablet.model || "--";


    // ==========================================
    // INFORMACIÓN DEL ACTIVO
    // ==========================================

    document.getElementById("detalleEmpleado").textContent =
        activoInfo?.empleadoAsig || "--";

    document.getElementById("detalleCodigo").textContent =
        activoInfo?.codigoEmp || "--";

    document.getElementById("detalleArea").textContent =
        activoInfo?.area || "--";

    document.getElementById("detalleDepartamento").textContent =
        activoInfo?.departamento || "--";
    document.getElementById("detalleRam").textContent =
        tablet.ram_usage || "--";

    document.getElementById("detalleAlmacenamiento").textContent =
        tablet.storage_usage || "--";

    document.getElementById("detalleImei").textContent =
        tablet.imei || "--";


    // ==========================================
    // ÚLTIMO REPORTE
    // ==========================================

    document.getElementById("detalleUltimoReporte").textContent =
        tablet.last_connection
            ? formatearFechaAuditoria(tablet.last_connection)
            : "Sin reporte";


    // ==========================================
    // SISTEMA ANDROID
    // ==========================================

    document.getElementById("detalleAndroid").textContent =
        tablet.os_version || "--";

    document.getElementById("detalleParcheSeguridad").textContent =
        tablet.security_patch || "--";


    // ==========================================
    // ACTUALIZACIÓN DEL SISTEMA
    // ==========================================

    const actualizacion =
        document.getElementById("detalleActualizacion");

    const filaActualizacion =
        document.getElementById("detalleFilaActualizacion");

    const estadoTitulo =
        document.getElementById("detalleEstadoTitulo");

    const estadoDescripcion =
        document.getElementById("detalleEstadoDescripcion");

    const estadoGeneral =
        document.getElementById("detalleEstadoGeneral");


    // ==========================================
    // ESTADO GENERAL DEL DISPOSITIVO
    // ==========================================

    let estaEnLinea = false;

    if (tablet.last_connection) {

        const ultimaConexion =
            new Date(tablet.last_connection);

        const ahora = new Date();

        const diferenciaMinutos =
            (ahora - ultimaConexion) / 1000 / 60;

        // Tu regla del MDM: offline después de 15 minutos
        estaEnLinea = diferenciaMinutos <= 15;
    }


    // ==========================================
    // 1. DISPOSITIVO FUERA DE LÍNEA
    // ==========================================

    if (!estaEnLinea) {

        estadoTitulo.textContent =
            "Dispositivo fuera de línea";

        estadoDescripcion.textContent =
            tablet.last_connection
                ? "Último reporte: " +
                formatearFechaAuditoria(tablet.last_connection)
                : "El dispositivo todavía no ha reportado conexión.";

        estadoGeneral.classList.remove(
            "estado-atencion"
        );

        estadoGeneral.classList.add(
            "estado-offline"
        );


        // ==========================================
        // 2. EN LÍNEA + ACTUALIZACIÓN PENDIENTE
        // ==========================================

    } else if (tablet.system_update_pending === true) {

        estadoTitulo.textContent =
            "Se requiere atención";

        estadoDescripcion.textContent =
            "Hay una actualización del sistema pendiente.";

        estadoGeneral.classList.remove(
            "estado-offline"
        );

        estadoGeneral.classList.add(
            "estado-atencion"
        );


        // ==========================================
        // 3. EN LÍNEA + TODO CORRECTO
        // ==========================================

    } else {

        estadoTitulo.textContent =
            "Todo está bien";

        estadoDescripcion.textContent =
            "El dispositivo está en línea";

        estadoGeneral.classList.remove(
            "estado-offline"
        );

        estadoGeneral.classList.remove(
            "estado-atencion"
        );
    }


    // ==========================================
    // ESTADO DE ACTUALIZACIÓN
    // ==========================================

    if (tablet.system_update_pending === true) {

        actualizacion.textContent =
            "Actualización pendiente";

        filaActualizacion.classList.add(
            "actualizacion-pendiente"
        );

    } else {

        actualizacion.textContent =
            "Sin actualización pendiente";

        filaActualizacion.classList.remove(
            "actualizacion-pendiente"
        );
    }
}

async function guardarDetalleDispositivo() {

    if (!detalleTabletId) {

        mostrarToastDetalle(
            "error",
            "No se pudo guardar",
            "No se ha seleccionado ningún dispositivo."
        );

        return;
    }


    // ==========================
    // DETECTAR PESTAÑA ACTIVA
    // ==========================

    const tabBateria =
        document.getElementById("detalle-bateria");

    const tabDispositivo =
        document.getElementById("detalle-dispositivo");


    // BATERÍA
    if (tabBateria.classList.contains("active")) {

        await guardarDetalleBateria();
        return;
    }


    // DISPOSITIVO
    if (tabDispositivo.classList.contains("active")) {

        mostrarToastDetalle(
            "warning",
            "Sin cambios",
            "No hay información del dispositivo para guardar."
        );

        return;
    }
}

async function guardarDetalleBateria() {

    const estado =
        document.getElementById("detalleEstadoBateria").value;

    let porcentaje = null;


    // ==========================
    // VALIDAR BATERÍA INFLADA
    // ==========================

    if (estado === "INFLADA") {

        const valor =
            document.getElementById("detallePorcentajeInflado").value;

        if (!valor) {

            mostrarToastDetalle(
                "warning",
                "Falta información",
                "Selecciona el porcentaje de inflado."
            );

            return;
        }

        porcentaje = parseInt(valor);
    }


    const datos = {
        estado_bateria: estado,
        porcentaje_inflado: porcentaje,
        usuario: sessionStorage.getItem("usuario")
    };

    try {

        const response = await fetch(
            `/devices/bateria/${detalleTabletId}`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(datos)
            }
        );


        if (!response.ok) {

            const mensaje = await response.text();

            throw new Error(
                mensaje || "No se pudo guardar el estado de batería"
            );
        }


        const tablet = await response.json();

        console.log(
            "Estado de batería actualizado:",
            tablet
        );


        // ==========================
        // MENSAJE
        // ==========================

        mostrarToastDetalle(
            "success",
            "Cambios guardados",
            "El estado de la batería se actualizó correctamente."
        );


        // ==========================
        // CERRAR MODAL
        // ==========================

        const modalElement =
            document.getElementById("modalDetalle");

        const modal =
            bootstrap.Modal.getInstance(modalElement);

        if (modal) {
            modal.hide();
        }


    } catch (error) {

        console.error(
            "Error guardando batería:",
            error
        );

        mostrarToastDetalle(
            "error",
            "Error al guardar",
            "No fue posible actualizar el estado de la batería."
        );
    }
}

function mostrarToastDetalle(tipo, titulo, mensaje) {

    const toastElement =
        document.getElementById("toastDetalle");

    if (!toastElement) {
        console.error("No existe #toastDetalle");
        return;
    }

    // IMPORTANTE:
    // Sacar el contenedor del toast del contenido dinámico/modal
    // y colocarlo directamente en el BODY.
    const toastContainer = toastElement.closest(".toast-container");

    if (
        toastContainer &&
        toastContainer.parentElement !== document.body
    ) {
        document.body.appendChild(toastContainer);
    }


    const icono =
        document.getElementById("toastDetalleIcono");

    const tituloElement =
        document.getElementById("toastDetalleTitulo");

    const mensajeElement =
        document.getElementById("toastDetalleMensaje");


    tituloElement.textContent = titulo;
    mensajeElement.textContent = mensaje;


    if (tipo === "success") {

        icono.className =
            "bi bi-check-circle-fill text-success me-2";

    } else if (tipo === "warning") {

        icono.className =
            "bi bi-exclamation-triangle-fill text-warning me-2";

    } else {

        icono.className =
            "bi bi-x-circle-fill text-danger me-2";
    }


    // Asegurar que quede por encima de TODO
    toastContainer.style.zIndex = "20000";


    const toast =
        bootstrap.Toast.getOrCreateInstance(
            toastElement,
            {
                autohide: true,
                delay: 3500
            }
        );

    toast.show();
}

function formatearFechaAuditoria(fecha) {

    if (!fecha) return "Sin registro";

    const date = new Date(fecha);

    const dia = String(date.getDate()).padStart(2, "0");
    const mes = String(date.getMonth() + 1).padStart(2, "0");
    const anio = date.getFullYear();

    const hora = String(date.getHours()).padStart(2, "0");
    const minutos = String(date.getMinutes()).padStart(2, "0");

    return `${dia}/${mes}/${anio} ${hora}:${minutos}`;
}

async function cargarAuditoriaBateria(activo) {

    // Limpiar primero
    document.getElementById("bateriaUltimoUsuario").textContent =
        "Sin registro";

    document.getElementById("bateriaUltimaFecha").textContent = "-";
    document.getElementById("bateriaUltimoDetalle").textContent = "";

    document.getElementById("bateriaAnteriorUsuario").textContent =
        "Sin registro";

    document.getElementById("bateriaAnteriorFecha").textContent = "-";
    document.getElementById("bateriaAnteriorDetalle").textContent = "";

    try {

        const response = await fetch(
            `/devices/auditoria/BATERIA/${activo}`
        );

        if (response.status === 204) {
            return;
        }

        if (!response.ok) {
            throw new Error("Error consultando auditoría");
        }

        const auditoria = await response.json();

        // ÚLTIMA
        document.getElementById("bateriaUltimoUsuario").textContent =
            auditoria.ultimoUsuario || "Sin registro";

        document.getElementById("bateriaUltimaFecha").textContent =
            formatearFechaAuditoria(auditoria.ultimaFecha);

        document.getElementById("bateriaUltimoDetalle").textContent =
            auditoria.ultimoDetalle || "";


        // ANTERIOR
        document.getElementById("bateriaAnteriorUsuario").textContent =
            auditoria.anteriorUsuario || "Sin registro";

        document.getElementById("bateriaAnteriorFecha").textContent =
            formatearFechaAuditoria(auditoria.anteriorFecha);

        document.getElementById("bateriaAnteriorDetalle").textContent =
            auditoria.anteriorDetalle || "";

    } catch (error) {

        console.error(
            "Error cargando auditoría de batería:",
            error
        );
    }
}


async function ejecutarDiagnosticoDispositivo() {

    // ==========================================
    // OBTENER ID DE LA TABLET ABIERTA
    // ==========================================

    if (!detalleTabletId) {

        console.error("No hay dispositivo seleccionado");

        mostrarToastDetalle(
            "error",
            "Diagnóstico",
            "No se ha seleccionado ningún dispositivo."
        );

        return;
    }

    const id = detalleTabletId;

    const boton =
        document.getElementById("btnEjecutarDiagnostico");


    // ==========================================
    // ESTADO: EJECUTANDO
    // ==========================================

    boton.disabled = true;

    boton.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2"></span>
        Ejecutando...
    `;


    document.getElementById("diagnosticoWebSocket").innerHTML =
        `<span class="text-muted">Comprobando...</span>`;

    document.getElementById("diagnosticoComando").innerHTML =
        `<span class="text-muted">Comprobando...</span>`;

    document.getElementById("diagnosticoAck").innerHTML =
        `<span class="text-muted">Esperando...</span>`;

    document.getElementById("diagnosticoLatencia").textContent =
        "--";


    const resultado =
        document.getElementById("diagnosticoResultado");

    const icono =
        document.getElementById("diagnosticoResultadoIcono");

    const titulo =
        document.getElementById("diagnosticoResultadoTitulo");

    const mensaje =
        document.getElementById("diagnosticoResultadoMensaje");


    resultado.className =
        "alert alert-info d-flex align-items-center mb-3";

    icono.className =
        "bi bi-arrow-repeat fs-4 me-3";

    titulo.textContent =
        "Ejecutando diagnóstico";

    mensaje.textContent =
        "Comprobando la comunicación con el dispositivo...";


    try {

        // ==========================================
        // LLAMAR A SPRING
        // ==========================================

        const response = await fetch(
            `/devices/${id}/diagnostico`
        );

        if (!response.ok) {

            throw new Error(
                `HTTP ${response.status}`
            );
        }


        const data = await response.json();

        // ==========================================
        // WEBSOCKET
        // ==========================================

        mostrarEstadoDiagnostico(
            "diagnosticoWebSocket",
            data.webSocket
        );


        // ==========================================
        // COMANDO
        // ==========================================

        mostrarEstadoDiagnostico(
            "diagnosticoComando",
            data.commandSent
        );


        // ==========================================
        // ACK
        // ==========================================

        mostrarEstadoDiagnostico(
            "diagnosticoAck",
            data.ackReceived
        );


        // ==========================================
        // LATENCIA
        // ==========================================

        const latencia =
            document.getElementById(
                "diagnosticoLatencia"
            );

        if (
            data.ackReceived &&
            data.latencyMs >= 0
        ) {

            latencia.innerHTML =
                `<span class="text-success fw-bold">
                    ${data.latencyMs} ms
                </span>`;

        } else {

            latencia.innerHTML =
                `<span class="text-danger fw-bold">
                    Sin respuesta
                </span>`;
        }


        // ==========================================
        // RESULTADO GENERAL
        // ==========================================

        if (data.success) {

            resultado.className =
                "alert alert-success d-flex align-items-center mb-3";

            icono.className =
                "bi bi-check-circle-fill fs-4 me-3";

            titulo.textContent =
                "Comunicación remota operativa";

            mensaje.textContent =
                `La tablet respondió correctamente al servidor en ${data.latencyMs} ms.`;

        } else {

            resultado.className =
                "alert alert-danger d-flex align-items-center mb-3";

            icono.className =
                "bi bi-exclamation-triangle-fill fs-4 me-3";

            titulo.textContent =
                "Problema de comunicación";

            mensaje.textContent =
                data.message ??
                "La tablet no respondió correctamente.";
        }


    } catch (error) {

        console.error(
            "ERROR DIAGNÓSTICO:",
            error
        );


        resultado.className =
            "alert alert-danger d-flex align-items-center mb-3";

        icono.className =
            "bi bi-x-circle-fill fs-4 me-3";

        titulo.textContent =
            "No se pudo ejecutar el diagnóstico";

        mensaje.textContent =
            "Ocurrió un error comunicándose con el servidor.";


    } finally {

        // ==========================================
        // RESTAURAR BOTÓN
        // ==========================================

        boton.disabled = false;

        boton.innerHTML = `
            <i class="bi bi-play-circle me-2"></i>
            Ejecutar diagnóstico
        `;
    }
}

function mostrarEstadoDiagnostico(
    elementoId,
    estado
) {

    const elemento =
        document.getElementById(elementoId);

    if (estado === true) {

        elemento.innerHTML = `
            <span class="text-success fw-bold">
                <i class="bi bi-check-circle-fill me-1"></i>
                OK
            </span>
        `;

    } else {

        elemento.innerHTML = `
            <span class="text-danger fw-bold">
                <i class="bi bi-x-circle-fill me-1"></i>
                ERROR
            </span>
        `;
    }
}


function formatearFechaDiagnostico(fecha) {

    if (!fecha) {
        return "--";
    }

    try {

        const date =
            new Date(fecha);

        return date.toLocaleString(
            "es-GT"
        );

    } catch (error) {

        return fecha;
    }
}