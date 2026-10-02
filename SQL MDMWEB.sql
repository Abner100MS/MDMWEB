
CREATE TABLE "Monitoreo tablet".adguard_politica_tablet (
    id BIGSERIAL PRIMARY KEY,
    tablet_id BIGINT NOT NULL,
    dominio VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL
);

CREATE TABLE "Monitoreo tablet".rol_acceso (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    descripcion VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

SELECT current_database(),
       current_schema(),
       inet_server_addr(),
       inet_server_port(),
       version();



DROP SCHEMA IF EXISTS "Monitoreo tablet" CASCADE;


CREATE SCHEMA "monitoreo tablet";

SELECT schema_name
FROM information_schema.schemata
WHERE schema_name = 'monitoreo tablet';


select *from activo_info 
Value activo;
INSERT INTO public.activo_info (
    activo,
    codigo_emp,
    empleado_asig,
    planta,
    area,
    departamento
)
values
('7364','12345','Juan Pérez','Planta 1','Pegado','Producción'),
('9847','54321','María López','Planta 2','Laminado','Calidad');


CREATE INDEX IF NOT EXISTS idx_dispositivos_activo
ON "monitoreo tablet".dispositivos(activo);

CREATE INDEX IF NOT EXISTS idx_activo_info_activo
ON public.activo_info(activo);

ALTER TABLE "monitoreo tablet".dispositivos
ADD COLUMN categoria VARCHAR(20);



INSERT INTO "monitoreo tablet".dispositivos (
    activo,
    android_id,
    device_name,
    model,
    categoria,
    battery_level,
    temperatura,
    estado_cargador,
    estado_wifi,
    estado_red,
    ip_address,
    ram_usage,
    storage_usage,
    uptime,
    last_connection,
    os_version
)
SELECT
    'TEST-' || gs,
    md5(random()::text),
    CASE (gs % 4)
        WHEN 0 THEN 'Galaxy Tab A8'
        WHEN 1 THEN 'Galaxy Tab S6 Lite'
        WHEN 2 THEN 'Redmi Note 12'
        ELSE 'Zebra TC21'
    END,
    CASE (gs % 4)
        WHEN 0 THEN 'SM-X200'
        WHEN 1 THEN 'SM-P610'
        WHEN 2 THEN '2201116TG'
        ELSE 'TC21'
    END,
    CASE (gs % 4)
        WHEN 0 THEN 'TABLET'
        WHEN 1 THEN 'TABLET'
        WHEN 2 THEN 'CELULAR'
        ELSE 'HANDHELD'
    END,
    (random()*100)::int,
    round((28 + random()*8)::numeric,1) || ' °C',
    CASE
        WHEN random() < 0.30 THEN
            'Desconectado desde ' || to_char(now(),'DD/MM/YY HH24:MI')
        ELSE
            'Conectado'
    END,
    'Conectado a WIFI-' || (1 + (random()*10)::int),
    'No Autorizado',
    '192.168.1.' || (10 + (random()*220)::int),
    (1 + random()*2)::numeric(3,1) || 'GB / 4GB',
    (5 + random()*50)::numeric(4,1) || 'GB / 64GB',
    (random()*80)::int || ' horas, ' || (random()*59)::int || ' min',
    now() - ((random()*720)::int || ' minutes')::interval,
    CASE
        WHEN random() < 0.5 THEN 'Android 13'
        ELSE 'Android 14'
    END
FROM generate_series(1,1500) gs;


DELETE
FROM "monitoreo tablet".dispositivos
WHERE activo LIKE 'TEST-%';


DROP TABLE IF EXISTS "monitoreo tablet".tarea_programada_dispositivo CASCADE;
DROP TABLE IF EXISTS "monitoreo tablet".tarea_programada CASCADE;

CREATE TABLE "monitoreo tablet".tarea_programada (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    tipo_tarea VARCHAR(50) NOT NULL,
    destino_tarea VARCHAR(30) NOT NULL,
    valor_destino VARCHAR(150),
    fecha_programada DATE NOT NULL,
    hora_programada TIME NOT NULL,
    parametros TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ejecucion TIMESTAMP
);

CREATE TABLE "monitoreo tablet".tarea_programada_dispositivo (
    id BIGSERIAL PRIMARY KEY,
    tarea_programada_id BIGINT NOT NULL,
    dispositivo_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_ejecucion TIMESTAMP,

    CONSTRAINT fk_tarea_programada
        FOREIGN KEY (tarea_programada_id)
        REFERENCES "monitoreo tablet".tarea_programada(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_dispositivo
        FOREIGN KEY (dispositivo_id)
        REFERENCES "monitoreo tablet".dispositivos(id)
        ON DELETE CASCADE
);


CREATE TABLE usuarios (

    id SERIAL PRIMARY KEY,

    usuario VARCHAR(50) UNIQUE NOT NULL,

    password VARCHAR(255) NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    rol VARCHAR(20) NOT NULL,

    activo BOOLEAN DEFAULT TRUE,

    fecha_creacion TIMESTAMP DEFAULT NOW()

);

UPDATE activo_info 
SET activo = 'TEST-11' 
WHERE activo = '12018' AND codigo_emp = '7490';


SELECT usuario, password
FROM "monitoreo tablet".usuarios;

CREATE INDEX idx_tarea_estado
ON "monitoreo tablet".tarea_programada (estado);

CREATE INDEX idx_tarea_fecha_hora
ON "monitoreo tablet".tarea_programada (fecha_programada, hora_programada);

CREATE INDEX idx_tarea_dispositivo
ON "monitoreo tablet".tarea_programada_dispositivo (dispositivo_id);


ALTER TABLE "monitoreo tablet".tarea_programada
ADD COLUMN tipo_programacion VARCHAR(20) NOT NULL DEFAULT 'UNA_VEZ';

ALTER TABLE "monitoreo tablet".tarea_programada
ADD COLUMN intervalo INTEGER;

ALTER TABLE "monitoreo tablet".tarea_programada
ADD COLUMN dias_semana VARCHAR(30);

ALTER TABLE "monitoreo tablet".tarea_programada
ADD COLUMN dia_mes INTEGER;

ALTER TABLE "monitoreo tablet".tarea_programada
ADD COLUMN proxima_ejecucion TIMESTAMP;


SELECT COUNT(*)
FROM dispositivos;

SELECT COUNT(*)
FROM dispositivos d
LEFT JOIN activo_info a
    ON d.activo = a.activo;

SELECT COUNT(DISTINCT activo) AS total_activos
FROM dispositivos;


SELECT COUNT(*)
FROM dispositivos
WHERE activo IS NULL;

SELECT COUNT(*)
FROM dispositivos
WHERE TRIM(COALESCE(activo,'')) = '';


SELECT *
FROM dispositivos
WHERE activo IS NULL
   OR TRIM(COALESCE(activo,'')) = '';


UPDATE activo_info
SET planta = 'PC'
WHERE planta = 'PLANTA-CENTRAL';

UPDATE activo_info
SET planta = 'PF'
WHERE planta IS NULL;


CREATE TABLE historial_cargador (
    id BIGSERIAL PRIMARY KEY,

    tablet_id BIGINT NOT NULL,

    estado_cargador VARCHAR(20) NOT NULL,

    porcentaje_bateria INTEGER,

    fecha_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_cargador_dispositivo
        FOREIGN KEY (tablet_id)
        REFERENCES dispositivos(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_historial_cargador_tablet_fecha
ON historial_cargador(tablet_id, fecha_evento DESC);


SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'historial_cargador';







CREATE TABLE reglas_apps (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE reglas_apps_detalle (
    id BIGSERIAL PRIMARY KEY,
    regla_id BIGINT NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),

    CONSTRAINT fk_regla_apps_detalle
        FOREIGN KEY (regla_id)
        REFERENCES reglas_apps(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_regla_package
        UNIQUE (regla_id, package_name)
);


CREATE TABLE reglas_apps_destinos (
    id BIGSERIAL PRIMARY KEY,
    regla_id BIGINT NOT NULL,
    tipo_destino VARCHAR(20) NOT NULL,
    valor_destino VARCHAR(255),

    CONSTRAINT fk_regla_apps_destino
        FOREIGN KEY (regla_id)
        REFERENCES reglas_apps(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_tipo_destino
        CHECK (
            tipo_destino IN (
                'TODAS',
                'CATEGORIA',
                'PLANTA',
                'DISPOSITIVO'
            )
        )
);


CREATE TABLE reglas_apps_excepciones (
    id BIGSERIAL PRIMARY KEY,
    activo VARCHAR(255) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),
    accion VARCHAR(10) NOT NULL,

    CONSTRAINT chk_reglas_apps_excepciones_accion
        CHECK (
            accion IN (
                'PERMITIR',
                'EXCLUIR'
            )
        ),

    CONSTRAINT uq_regla_app_excepcion_activo
        UNIQUE (activo, package_name)
);


CREATE INDEX idx_reglas_apps_detalle_regla
ON reglas_apps_detalle(regla_id);


CREATE INDEX idx_reglas_apps_destinos_regla
ON reglas_apps_destinos(regla_id);


CREATE INDEX idx_reglas_apps_destinos_tipo_valor
ON reglas_apps_destinos(tipo_destino, valor_destino);


CREATE INDEX idx_reglas_apps_excepciones_activo
ON reglas_apps_excepciones(activo);






WITH nueva_regla AS (
    INSERT INTO reglas_apps (
        nombre,
        descripcion,
        activa
    )
    VALUES (
        'Aplicaciones Generales',
        'Aplicaciones permitidas por defecto en los dispositivos',
        TRUE
    )
    RETURNING id
),
destino AS (
    INSERT INTO reglas_apps_destinos (
        regla_id,
        tipo_destino,
        valor_destino
    )
    SELECT
        id,
        'TODAS',
        NULL
    FROM nueva_regla
)
INSERT INTO reglas_apps_detalle (
    regla_id,
    package_name,
    app_name
)
SELECT
    id,
    package_name,
    app_name
FROM nueva_regla
CROSS JOIN (
    VALUES

    ('com.android.chrome', 'Chrome'),
    ('org.mozilla.firefox', 'Firefox'),
    ('com.cxinventor.file.explorer', 'Cx Explorador de Archivos'),

    ('org.chromium.webapk.ac08264a7498f418b_v2', 'Escaneos Atomic'),
    ('com.orchservice', 'Atomic Notify'),

    ('com.microsoft.powerbim', 'Power BI'),

    ('com.sec.android.app.camera', 'Cámara'),
    ('com.android.camera', 'Cámara'),
    ('com.mediatek.camera', 'Cámara'),
    ('org.codeaurora.snapcam', 'Cámara'),

    ('com.sec.android.app.popupcalculator', 'Calculadora'),
    ('com.google.android.calculator', 'Calculadora'),
    ('com.miui.calculator', 'Calculadora'),
    ('com.miui.calculator.go', 'Calculadora'),

    ('com.samsung.android.calendar', 'Calendario'),
    ('com.xiaomi.calendar', 'Calendario'),
    ('com.android.calendar', 'Calendario'),

    ('com.sec.android.gallery3d', 'Galería'),
    ('com.miui.gallery', 'Galería'),

    ('com.sec.android.app.myfiles', 'Mis Archivos'),
    ('com.google.android.documentsui', 'Archivos'),
    ('com.google.android.go.documentsui', 'Archivos'),
    ('com.mi.android.globalFileexplorer', 'Administrador de archivos'),

    ('com.sec.android.app.clockpackage', 'Reloj'),
    ('com.google.android.deskclock', 'Reloj'),
    ('com.android.deskclock', 'Reloj'),

    ('com.google.android.apps.photos', 'Fotos')

) AS apps(package_name, app_name);




SELECT
    r.id,
    r.nombre,
    r.activa,
    d.tipo_destino,
    a.app_name,
    a.package_name
FROM reglas_apps r
LEFT JOIN reglas_apps_destinos d
    ON d.regla_id = r.id
LEFT JOIN reglas_apps_detalle a
    ON a.regla_id = r.id
ORDER BY r.id, a.app_name, a.package_name;



INSERT INTO reglas_apps_excepciones (
    activo,
    package_name,
    app_name,
    accion
)
SELECT
    '12528',
    package_name,
    app_name,
    'EXCLUIR'
FROM reglas_apps_detalle
WHERE LOWER(app_name) = 'mis archivos'
ON CONFLICT (activo, package_name)
DO UPDATE SET
    app_name = EXCLUDED.app_name,
    accion = 'EXCLUIR';



SELECT
    activo,
    app_name,
    package_name,
    accion
FROM reglas_apps_excepciones
WHERE activo = '10588';


DELETE FROM reglas_apps_excepciones
WHERE activo = '12528'
  AND LOWER(app_name) = 'mis archivos';


SELECT *
FROM reglas_apps_excepciones
WHERE activo = '10588';





SELECT
    regla_id,
    package_name,
    app_name
FROM reglas_apps_detalle
WHERE package_name = 'com.google.android.calendar';


INSERT INTO reglas_apps_detalle (
    regla_id,
    package_name,
    app_name
)
VALUES (
    1,
    'com.google.android.calendar',
    'Calendario'
);




INSERT INTO reglas_apps_detalle (
    regla_id,
    package_name,
    app_name
)
VALUES (
    1,
    'com.google.android.apps.docs',
    'Drive'
);


ALTER TABLE "monitoreo tablet".dispositivos
RENAME COLUMN codigo_emp TO estado_bateria;

ALTER TABLE "monitoreo tablet".dispositivos
RENAME COLUMN nombre_emp TO porcentaje_inflado;

UPDATE "monitoreo tablet".dispositivos
SET estado_bateria = 'NORMAL',
    porcentaje_inflado = NULL;


CREATE TABLE "monitoreo tablet".auditoria_dispositivos (

    id BIGSERIAL PRIMARY KEY,

    activo VARCHAR(100) NOT NULL,

    accion VARCHAR(100) NOT NULL,

    -- ÚLTIMA MODIFICACIÓN
    ultimo_usuario VARCHAR(150),
    ultima_fecha TIMESTAMP,
    ultimo_detalle VARCHAR(500),

    -- MODIFICACIÓN ANTERIOR
    anterior_usuario VARCHAR(150),
    anterior_fecha TIMESTAMP,
    anterior_detalle VARCHAR(500),

    CONSTRAINT uq_auditoria_activo_accion
        UNIQUE (activo, accion)
);

SELECT
    SUM(
        CASE
            WHEN UPPER(COALESCE(estado_bateria, 'NORMAL')) = 'INFLADA'
            THEN 1 ELSE 0
        END
    ) AS bateriasInfladas
FROM "monitoreo tablet".dispositivos;



ALTER TABLE dispositivos
ADD COLUMN security_patch VARCHAR(20);

ALTER TABLE dispositivos
ADD COLUMN system_update_pending BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE dispositivos
ADD COLUMN system_update_received_time BIGINT;


ALTER TABLE "monitoreo tablet".dispositivos
ADD COLUMN app_version VARCHAR(30);

ALTER TABLE "monitoreo tablet".dispositivos
ADD COLUMN imei VARCHAR(30);

SELECT
    activo,
    app_version,
    imei
FROM "monitoreo tablet".dispositivos
WHERE activo = '10677';



CREATE TABLE mdm_wallpapers (
    id BIGSERIAL PRIMARY KEY,

    nombre VARCHAR(150) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,

    tipo_mime VARCHAR(100),
    tamano_bytes BIGINT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_por VARCHAR(100),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE mdm_wallpaper_asignaciones (
    id BIGSERIAL PRIMARY KEY,

    wallpaper_id BIGINT NOT NULL,

    tipo_destino VARCHAR(30) NOT NULL,
    valor_destino VARCHAR(150),

    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asignado_por VARCHAR(100),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_wallpaper_asignacion
        FOREIGN KEY (wallpaper_id)
        REFERENCES mdm_wallpapers(id)
);


CREATE TABLE mdm_wallpaper_dispositivos (
    id BIGSERIAL PRIMARY KEY,

    wallpaper_id BIGINT NOT NULL,
    tablet_id BIGINT NOT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',

    fecha_envio TIMESTAMP,
    fecha_confirmacion TIMESTAMP,

    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(500),

    CONSTRAINT fk_wallpaper_dispositivo_wallpaper
        FOREIGN KEY (wallpaper_id)
        REFERENCES mdm_wallpapers(id),

    CONSTRAINT fk_wallpaper_dispositivo_tablet
        FOREIGN KEY (tablet_id)
        REFERENCES dispositivos(id),

    CONSTRAINT uq_wallpaper_tablet
        UNIQUE (wallpaper_id, tablet_id)
);


ALTER TABLE dispositivos
ADD COLUMN wallpaper_id BIGINT,
ADD COLUMN wallpaper_fecha TIMESTAMP;


ALTER TABLE mdm_wallpapers
ADD COLUMN imagen BYTEA;


ALTER TABLE mdm_wallpapers
DROP COLUMN ruta_archivo;



CREATE TABLE stock (
    id BIGSERIAL PRIMARY KEY,
    activo VARCHAR(100) NOT NULL UNIQUE,
    fecha_ingreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_ingreso TEXT NOT NULL,
    condicion VARCHAR(30) NOT NULL DEFAULT 'BUENO',
    observacion TEXT
);


CREATE TABLE historial_stock (
    id BIGSERIAL PRIMARY KEY,
    activo VARCHAR(100) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comentario TEXT
);



SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'stock';




SELECT
    d.activo,
    d.last_connection,
    s.fecha_ingreso,
    CASE
        WHEN d.last_connection > s.fecha_ingreso
        THEN 'SI - REPORTO DESPUES DE STOCK'
        ELSE 'NO - NO HA REPORTADO DESPUES DE STOCK'
    END AS resultado
FROM "monitoreo tablet".dispositivos d
JOIN "monitoreo tablet".stock s
    ON TRIM(s.activo) = TRIM(d.activo)
WHERE TRIM(d.activo) = '12697';




SELECT
    activo,
    last_connection,
    battery_level,
    temperatura,
    ram_usage,
    storage_usage,
    uptime
FROM "monitoreo tablet".dispositivos
WHERE TRIM(activo) = '12697';

ALTER TABLE "monitoreo tablet".dispositivos
ADD COLUMN sin_respuesta BOOLEAN NOT NULL DEFAULT FALSE;



SELECT
    d.id,
    d.activo,
    d.last_connection,
    d.sin_respuesta,
    s.fecha_ingreso,
    CASE
        WHEN d.last_connection > s.fecha_ingreso
        THEN 'SI'
        ELSE 'NO'
    END AS reporto_despues_de_stock,
    CASE
        WHEN d.last_connection >= NOW() - INTERVAL '17 minutes'
        THEN 'SI'
        ELSE 'NO'
    END AS heartbeat_reciente
FROM "monitoreo tablet".dispositivos d
INNER JOIN "monitoreo tablet".stock s
    ON TRIM(s.activo) = TRIM(d.activo);