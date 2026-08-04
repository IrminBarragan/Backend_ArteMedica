export interface CompraDetalleRequestDTO {
  productoId: number;
  numeroLote: string;
  fechaCaducidad: string;
  cantidad: number;
  costoUnitario: number;
}

export interface CompraRequestDTO {
  proveedorId: number;
  numeroFactura: string;
  fechaCompra: string;
  detalles: CompraDetalleRequestDTO[];
}

export interface CompraDetalleResponseDTO {
  productoId: number;
  productoNombre: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
  loteId: number | null;
  numeroLote: string | null;
}

export interface CompraResponseDTO {
  id: number;
  proveedorId: number;
  proveedorNombre: string;
  numeroFactura: string;
  fechaCompra: string;
  usuarioRegistroUsername: string;
  total: number;
  detalles: CompraDetalleResponseDTO[];
  createdAt: string;
}
