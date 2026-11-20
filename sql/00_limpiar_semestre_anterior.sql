-- =====================================================================
--  ¡DESTRUCTIVO! Elimina las tablas y vistas del proyecto del semestre
--  anterior (sistema de veterinaria) que quedaron en db_grupo23sa.
--  Varias chocan con el esquema nuevo (roles, permisos, bitacora...).
--  Ejecutar UNA sola vez, antes de 01_esquema.sql, y solo en db_grupo23sa.
-- =====================================================================
BEGIN;

DROP VIEW IF EXISTS v_historial_mascotas, v_historial_vacunas, v_stock_bajo, v_vacunas_proximas CASCADE;

DROP TABLE IF EXISTS
    almacenes, bitacora, categorias, clientes, consultas_medicas, cuotas_credito,
    detalles_nota_compra, detalles_nota_venta, especies, historial_vacunacion,
    inventarios, mascotas, menus, movimientos_inventario, notas_compra, notas_venta,
    pagos, password_reset_tokens, permisos, personal_access_tokens, productos,
    proveedores, razas, roles, roles_permisos, servicios, sessions, tratamientos,
    usuarios, vacunas, veterinarios, visitas
CASCADE;

COMMIT;
