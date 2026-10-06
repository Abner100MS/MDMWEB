

async function iniciarDashboard() {

    const res = await fetch("/devices/dashboard");

    const d = await res.json();


    // Indicadores

    document.getElementById("dash-total").textContent = d.total;

    document.getElementById("dash-online").textContent =
        d.total - d.offline;

    document.getElementById("dash-offline").textContent =
        d.offline;

    const porcentajeOnline =
        d.total === 0
            ? "0.00"
            : (((d.total - d.offline) * 100) / d.total).toFixed(2);

    const porcentajeOffline =
        d.total === 0
            ? "0.00"
            : ((d.offline * 100) / d.total).toFixed(2);

    document.getElementById("dash-bateria").textContent =
        d.bateriaBaja;

    const porcentajeBateria =
        d.total === 0
            ? "0.00"
            : ((d.bateriaBaja * 100) / d.total).toFixed(2);

    const totalBateria = d.total || 1;

    function porcentaje(valor) {
        return ((valor * 100) / totalBateria).toFixed(1);
    }

    document.getElementById("dash-online-porcentaje").textContent =
        porcentajeOnline + "% del total";

    document.getElementById("dash-offline-porcentaje").textContent =
        porcentajeOffline + "% del total";

    document.getElementById("bat-alta").textContent =
        `${d.bateria80a100} (${porcentaje(d.bateria80a100)}%)`;

    document.getElementById("bat-media").textContent =
        `${d.bateria50a79} (${porcentaje(d.bateria50a79)}%)`;

    document.getElementById("bat-baja").textContent =
        `${d.bateria20a49} (${porcentaje(d.bateria20a49)}%)`;

    document.getElementById("bat-critica").textContent =
        `${d.bateria0a19} (${porcentaje(d.bateria0a19)}%)`;

    //document.getElementById("bat-sindatos").textContent =
    //  d.bateriaSinDatos;

    document.getElementById("bat-total").textContent =
        d.total;

    document.getElementById("barra-bat-alta").style.width =
        porcentaje(d.bateria80a100) + "%";

    document.getElementById("barra-bat-media").style.width =
        porcentaje(d.bateria50a79) + "%";

    document.getElementById("barra-bat-baja").style.width =
        porcentaje(d.bateria20a49) + "%";

    document.getElementById("barra-bat-critica").style.width =
        porcentaje(d.bateria0a19) + "%";

    document.getElementById("dash-bateria-porcentaje").textContent =
        porcentajeBateria + "% del total";


    // Alertas

    document.getElementById("dash-cel").textContent =
        d.nopowerCel;

    document.getElementById("dash-hand").textContent =
        d.nopowerHand;

    document.getElementById("dash-calidad").textContent =
        d.nopowerTab;

    document.getElementById("dash-general").textContent =
        d.nopowerGen;

    document.getElementById("dash-alertas-total").textContent =
        d.nopowerCel +
        d.nopowerHand +
        d.nopowerTab +
        d.nopowerGen;

    // Gráficas

    crearDonut("chartPlantaGeneral", d.plantaGeneral);
    llenarTabla("tablaPlantaGeneral", d.plantaGeneral);

    crearDonut("chartPlantaPc", d.plantaPc);
    llenarTabla("tablaPlantaPc", d.plantaPc);

    crearDonut("chartPlantaPf", d.plantaPf);
    llenarTabla("tablaPlantaPf", d.plantaPf);

    crearDonut("chartCategorias", d.categorias);
    llenarTabla("tablaCategorias", d.categorias);

    crearDonut("chartEstadoRed", d.estadoRed);
    llenarTablaEstadoRed(d.estadoRed);
}

let chartPlantaGeneral = null;

function crearDonut(canvasId, datos) {

    const canvas = document.getElementById(canvasId);

    if (!canvas || !datos) return;

    if (canvas.chart) {
        canvas.chart.destroy();
    }


    datos.forEach(x => {

    });
    const centerTextPlugin = {

        id: "centerText",

        afterDraw(chart) {

            const { ctx } = chart;

            const meta = chart.getDatasetMeta(0);

            if (!meta.data.length) return;

            const x = meta.data[0].x;
            const y = meta.data[0].y;

            const total = chart.data.datasets[0].data
                .reduce((a, b) => a + b, 0);

            ctx.save();

            ctx.textAlign = "center";
            ctx.textBaseline = "middle";

            ctx.fillStyle = "#212529";
            ctx.font = "bold 26px Arial";
            ctx.fillText(total, x, y - 8);

            ctx.fillStyle = "#6c757d";
            ctx.font = "14px Arial";
            ctx.fillText("Total", x, y + 16);

            ctx.restore();

        }

    };

    canvas.chart = new Chart(canvas, {

        type: "doughnut",

        data: {

            labels: datos.map(x => x.nombre),

            datasets: [{

                data: datos.map(x => x.cantidad),

                backgroundColor: datos.map(x => obtenerColor(x.nombre)),

                borderColor: "#ffffff",

                borderWidth: 2

            }]

        },

        options: {

            responsive: true,

            maintainAspectRatio: false,

            cutout: "72%",

            plugins: {

                legend: {
                    display: false
                },

                title: {
                    display: false
                }

            }

        },

        plugins: [centerTextPlugin]

    });

}

function obtenerColor(nombre) {

    switch ((nombre || "").trim().toUpperCase()) {

        case "PC":
        case "PLANTA CINTURONES":
            return "#3b82f6";

        case "PF":
        case "PLANTA FAJAS":
            return "#22c55e";

        case "CALIDAD":
            return "#22c55e";

        case "CELULAR":
            return "#f59e0b";

        case "HANDHELD":
            return "#8b5cf6";

        case "GENERAL":
            return "#3b82f6";

        case "AUTORIZADO":
            return "#22c55e";

        case "NO AUTORIZADO":
            return "#ef4444";

        case "SIN DATOS":
            return "#94a3b8";

        default:
            return "#64748b";
    }
}
function llenarTabla(id, datos) {

    const contenedor = document.getElementById(id);

    if (!contenedor || !datos) return;

    contenedor.innerHTML = "";

    const datosMostrar =
        (id === "tablaPlantaPc" || id === "tablaPlantaPf")
            ? datos.filter(x => x.nombre.toUpperCase() !== "SIN DATOS")
            : datos;
    const total = datosMostrar.reduce((s, x) => s + x.cantidad, 0);

    datosMostrar.forEach(x => {

        const porcentaje = total === 0
            ? "0.0"
            : ((x.cantidad * 100) / total).toFixed(1);

        contenedor.innerHTML += `

<div class="dashboard-item">

    <div class="dashboard-item-nombre">

        <div class="dashboard-color"
             style="background:${obtenerColor(x.nombre)}">
        </div>

        <div class="dashboard-item-nombre-texto">

            ${(id === "tablaPlantaPc" || id === "tablaPlantaPf") &&
                x.nombre.toUpperCase() === "GENERAL"
                ? "General"
                : x.nombre}

        </div>

    </div>

    <span>

        ${x.cantidad}

    </span>

    <span>

        (${porcentaje}%)

    </span>

</div>

`;

    });

    contenedor.innerHTML += `

<div class="dashboard-total">

    <span>Total</span>

    <span>${total}</span>

    <span>(100%)</span>

</div>

`;

}

function llenarTablaEstadoRed(datos) {

    const contenedor = document.getElementById("tablaEstadoRed");

    if (!contenedor || !datos) return;

    contenedor.innerHTML = "";

    const total = datos.reduce((s, x) => s + x.cantidad, 0);

    datos.forEach(x => {

        const porcentaje = total === 0
            ? "0.0"
            : ((x.cantidad * 100) / total).toFixed(1);

        contenedor.innerHTML += `

        <div class="dashboard-item">

            <div class="dashboard-item-nombre">

                <div class="dashboard-color"
                     style="background:${obtenerColor(x.nombre)}">
                </div>

                <div class="dashboard-item-nombre-texto">

                    ${x.nombre}

                </div>

            </div>

            <span>${x.cantidad}</span>

            <span>(${porcentaje}%)</span>

        </div>

        `;

    });

    contenedor.innerHTML += `

    <div class="dashboard-total">

        <span>Total</span>

        <span>${total}</span>

        <span>(100%)</span>

    </div>

    `;

}


setInterval(() => {

    iniciarDashboard();

}, 60000);

//900000)