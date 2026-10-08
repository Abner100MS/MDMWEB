
/* =====================================================
   HISTORIAL DE INCIDENCIAS WI-FI
===================================================== */

const WIFI_API = "/api/wifi-incidencias";

let wifiPaginaActual = 0;
let wifiTotalPaginas = 0;
let wifiTotalElementos = 0;
let wifiFiltrosActuales = null;
let wifiConsultaVersion = 0;


// =====================================================
// MOSTRAR VISTA HISTORIAL WI-FI
// =====================================================

async function mostrarVistaHistorialWifi() {

    guardarVista("historial-wifi");

    vistaActual = "historial-wifi";

    detenerTrackingSiExiste();

    ocultarTodasLasVistas();

    document.getElementById("btnActualizar").style.display = "none";

    document.getElementById("vista-historial-wifi").style.display = "block";


    // ==========================================
    // MENÚ LATERAL
    // ==========================================

    document.querySelectorAll(".sidebar .nav-link").forEach(x => {

        x.classList.remove(
            "active",
            "text-white"
        );

        x.classList.add("text-secondary");

    });

    document.getElementById("menuHistorialWifi")
        .classList.remove("text-secondary");

    document.getElementById("menuHistorialWifi")
        .classList.add(
            "active",
            "text-white"
        );


    // ==========================================
    // CARGAR VISTA HISTORIAL WI-FI
    // ==========================================

    const contenedor =
        document.getElementById("vista-historial-wifi");

    if (!contenedor.dataset.cargado) {

        const res = await fetch("/modals/historial-wifi.html");

        if (!res.ok) {
            console.error("No se pudo cargar historial-wifi.html");
            return;
        }

        contenedor.innerHTML = await res.text();

        inicializarHistorialWifi();

        contenedor.dataset.cargado = "true";
    }

    // Mostrar resumen general o última búsqueda
    buscarHistorialWifi();
}


// =====================================================
// INICIALIZAR EVENTOS
// =====================================================

function inicializarHistorialWifi() {

    document.getElementById("btnBuscarHistorialWifi")
        .addEventListener("click", () => buscarHistorialWifi());


    document.getElementById("wifiPaginaAnterior")
        .addEventListener("click", () => cambiarPaginaWifi(-1));

    document.getElementById("wifiPaginaSiguiente")
        .addEventListener("click", () => cambiarPaginaWifi(1));

    const inputActivo = document.getElementById("wifiFiltroActivo");

    // BUSCAR CON ENTER
    inputActivo.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            buscarHistorialWifi();
        }
    });

    // RESTABLECER AL BORRAR EL ACTIVO
    inputActivo.addEventListener("input", () => {

        if (inputActivo.value.trim() === "") {
            limpiarHistorialWifi();
        }

    });

}



// =====================================================
// BUSCAR HISTORIAL WI-FI
// =====================================================
async function buscarHistorialWifi(pagina = 0) {

    const activo = document.getElementById("wifiFiltroActivo")
        .value.trim();

    const desde = document.getElementById("wifiFechaDesde").value;
    const hasta = document.getElementById("wifiFechaHasta").value;

    const verHistorial = document.getElementById("wifiVerHistorial")
        .checked;

    if (desde && hasta && desde > hasta) {
        alert("La fecha inicial no puede ser posterior a la final.");
        return;
    }

    // Si se solicita historial sin activo, pedir el activo.
    if (verHistorial && !activo) {
        alert("Ingresa un activo para ver su historial individual.");
        return;
    }

    /*
       LOS 5 CASOS:

       1. Sin activo ni fechas -> resumen general.
       2. Solo activo -> resumen del activo.
       3. Activo + Ver historial -> registros individuales.
       4. Activo + fechas -> registros individuales.
       5. Solo fechas -> resumen por activo.
    */

    const consultaIndividual = Boolean(
        activo && (verHistorial || desde || hasta)
    );

    const endpoint = consultaIndividual
        ? `${WIFI_API}/historial`
        : `${WIFI_API}/resumen`;

    const parametros = new URLSearchParams();

    if (activo) parametros.set("activo", activo);
    if (desde) parametros.set("desde", desde);
    if (hasta) parametros.set("hasta", hasta);

    parametros.set("pagina", pagina);

    wifiFiltrosActuales = {
        activo,
        desde,
        hasta,
        verHistorial
    };

    wifiPaginaActual = pagina;

    const version = ++wifiConsultaVersion;

    mostrarCargandoWifi();

    try {

        const respuesta = await fetch(
            `${endpoint}?${parametros.toString()}`
        );

        if (!respuesta.ok) {
            throw new Error(`Error HTTP ${respuesta.status}`);
        }

        const datos = await respuesta.json();

        // Ignorar respuestas anteriores si se realizó otra búsqueda.
        if (version !== wifiConsultaVersion) return;

        // Evitar modificar otra pantalla si se cambió de vista.
        if (!document.getElementById("wifiTablaBody")) return;

        wifiPaginaActual = datos.number ?? pagina;
        wifiTotalPaginas = datos.totalPages ?? 0;
        wifiTotalElementos = datos.totalElements ?? 0;

        renderizarTablaWifi(
            datos.content || [],
            consultaIndividual
        );

        actualizarPaginacionWifi();

    } catch (error) {

        if (version !== wifiConsultaVersion) return;

        console.error("Error consultando Wi-Fi:", error);

        mostrarMensajeTablaWifi(
            "No se pudo consultar el historial Wi-Fi."
        );
    }
}


// =====================================================
// RENDERIZAR TABLA
// =====================================================
function renderizarTablaWifi(registros, consultaIndividual) {

    const tbody = document.getElementById("wifiTablaBody");

    tbody.innerHTML = "";

    if (!registros.length) {
        mostrarMensajeTablaWifi(
            "No se encontraron incidencias Wi-Fi."
        );
        return;
    }

    registros.forEach(registro => {

        const fila = document.createElement("tr");

        const motivo = obtenerMotivoWifi(registro);

        const columnas = [
            registro.activo ?? "-",
            registro.ssid ?? "-",
            formatearFechaWifi(registro.fechaPerdida),
            formatearFechaWifi(registro.fechaRecuperacion),
            formatearDuracionWifi(registro.duracionSegundos),
            motivo,
            consultaIndividual
                ? wifiTotalElementos
                : (registro.totalRegistros ?? 0)
        ];

        columnas.forEach((valor, indice) => {

            const celda = document.createElement("td");

            if (indice === 0) {

                // ACTIVO CON ETIQUETA AZUL
                const badge = document.createElement("span");

                badge.className = "badge bg-primary";
                badge.textContent = valor;

                celda.appendChild(badge);

            } else {

                celda.textContent = valor;

            }

            if (indice === 6) {
                celda.classList.add("text-center");
            }

            fila.appendChild(celda);
        });

        tbody.appendChild(fila);
    });
}


// =====================================================
// CLASIFICAR MOTIVO DE DESCONEXIÓN
// =====================================================
function obtenerMotivoWifi(registro) {

    if (registro.wifiHabilitado === false) {
        return "Wi-Fi deshabilitado";
    }

    if (
        registro.wifiHabilitado === true &&
        registro.transporteWifi === false
    ) {
        return "Sin conexión Wi-Fi";
    }

    if (
        registro.transporteWifi === true &&
        registro.internetValidado === false
    ) {
        return "Sin Internet validado";
    }

    return "No determinado";
}


// =====================================================
// FORMATEAR FECHAS
// =====================================================
function formatearFechaWifi(milisegundos) {

    if (milisegundos === null ||
        milisegundos === undefined) {
        return "-";
    }

    const fecha = new Date(Number(milisegundos));

    if (isNaN(fecha.getTime())) {
        return "-";
    }

    return new Intl.DateTimeFormat("es-GT", {
        timeZone: "America/Guatemala",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(fecha);
}


// =====================================================
// FORMATEAR DURACIÓN
// =====================================================
function formatearDuracionWifi(segundos) {

    if (segundos === null || segundos === undefined) {
        return "-";
    }

    const total = Math.max(0, Number(segundos));

    if (!Number.isFinite(total)) {
        return "-";
    }

    const horas = Math.floor(total / 3600);
    const minutos = Math.floor((total % 3600) / 60);
    const segundosRestantes = Math.floor(total % 60);

    if (horas > 0) {
        return `${horas} h ${minutos} min ${segundosRestantes} s`;
    }

    if (minutos > 0) {
        return `${minutos} min ${segundosRestantes} s`;
    }

    return `${segundosRestantes} s`;
}


// =====================================================
// PAGINACIÓN
// =====================================================
function actualizarPaginacionWifi() {

    const total = wifiTotalElementos;
    const pagina = wifiPaginaActual;
    const paginas = wifiTotalPaginas;

    const inicio = total === 0 ? 0 : pagina * 10 + 1;
    const fin = Math.min((pagina + 1) * 10, total);

    document.getElementById("wifiTotalResultados")
        .textContent = `${total} resultados`;

    document.getElementById("wifiInfoPaginacion")
        .textContent = `Mostrando ${inicio}-${fin} de ${total} registros`;

    document.getElementById("wifiNumeroPagina")
        .textContent = `Página ${paginas === 0 ? 1 : pagina + 1} de ${Math.max(1, paginas)}`;

    document.getElementById("wifiPaginaAnterior")
        .disabled = pagina <= 0;

    document.getElementById("wifiPaginaSiguiente")
        .disabled = pagina >= paginas - 1;
}


// =====================================================
// CAMBIAR PÁGINA
// =====================================================
function cambiarPaginaWifi(direccion) {

    const nuevaPagina = wifiPaginaActual + direccion;

    if (nuevaPagina < 0 || nuevaPagina >= wifiTotalPaginas) {
        return;
    }

    // Conservar los filtros usados en la búsqueda.
    if (wifiFiltrosActuales) {

        document.getElementById("wifiFiltroActivo").value =
            wifiFiltrosActuales.activo;

        document.getElementById("wifiFechaDesde").value =
            wifiFiltrosActuales.desde;

        document.getElementById("wifiFechaHasta").value =
            wifiFiltrosActuales.hasta;

        document.getElementById("wifiVerHistorial").checked =
            wifiFiltrosActuales.verHistorial;
    }

    buscarHistorialWifi(nuevaPagina);
}


// =====================================================
// LIMPIAR FILTROS
// =====================================================
function limpiarHistorialWifi() {

    document.getElementById("wifiFiltroActivo").value = "";
    document.getElementById("wifiFechaDesde").value = "";
    document.getElementById("wifiFechaHasta").value = "";
    document.getElementById("wifiVerHistorial").checked = false;

    wifiPaginaActual = 0;

    buscarHistorialWifi(0);
}


// =====================================================
// MOSTRAR CARGANDO
// =====================================================
function mostrarCargandoWifi() {

    mostrarMensajeTablaWifi("Cargando incidencias Wi-Fi...");
}


// =====================================================
// MOSTRAR MENSAJE EN TABLA
// =====================================================
function mostrarMensajeTablaWifi(mensaje) {

    const tbody = document.getElementById("wifiTablaBody");

    if (!tbody) return;

    tbody.innerHTML = "";

    const fila = document.createElement("tr");
    const celda = document.createElement("td");

    celda.colSpan = 7;
    celda.className = "text-center text-muted py-4";
    celda.textContent = mensaje;

    fila.appendChild(celda);
    tbody.appendChild(fila);
}
