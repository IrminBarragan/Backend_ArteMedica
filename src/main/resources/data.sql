INSERT INTO configuracion_sistema (clave, valor, descripcion, updated_at, updated_by)
SELECT 'INVENTARIO_FEFO_HABILITADO', 'true', 'Si esta en true, las dispensaciones descuentan primero del lote con fecha de caducidad mas proxima (FEFO). Si esta en false, se usa FIFO por fecha de ingreso.', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_sistema WHERE clave = 'INVENTARIO_FEFO_HABILITADO');
