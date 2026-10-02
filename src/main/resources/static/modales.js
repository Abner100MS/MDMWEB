async function cargarModales() {

    try {

        const respuesta = await fetch("/modals/historial-cargador.html");

        if (!respuesta.ok) {
            throw new Error("No se pudo cargar el modal: " + respuesta.status);
        }

        const html = await respuesta.text();

        document.body.insertAdjacentHTML("beforeend", html);


    } catch (error) {

        console.error("Error cargando el modal:", error);

    }

}



let tabletHistorialActual = null;

async function cargarHistorialCargador(idTablet) {

    try {

        const fechaDesde = document.getElementById("fechaDesde").value;
        const fechaHasta = document.getElementById("fechaHasta").value;

        let url = `/devices/${idTablet}/historial-cargador`;

        const parametros = new URLSearchParams();

        if (fechaDesde) {
            parametros.append("fechaDesde", fechaDesde);
        }

        if (fechaHasta) {
            parametros.append("fechaHasta", fechaHasta);
        }

        if (parametros.toString() !== "") {
            url += "?" + parametros.toString();
        }

        const respuesta = await fetch(url);

        if (!respuesta.ok) {
            throw new Error("Error al obtener historial");
        }

        const datos = await respuesta.json();

        const tbody = document.getElementById("tablaHistorialCargador");

        tbody.innerHTML = "";

        if (datos.length === 0) {

            tbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-muted">
                        Sin registros
                    </td>
                </tr>
            `;

            return;
        }

        datos.forEach(item => {

            tbody.innerHTML += `
                <tr>

                    <td class="fw-bold">
                        ${item[0]}
                    </td>

                    <td>
                        <span class="fw-bold text-danger">
                            ${item[1]}
                        </span>
                    </td>

                    <td class="fw-bold">
                        ${item[2]}%
                    </td>

                </tr>
            `;

        });

    } catch (e) {

        console.error(e);

    }

}