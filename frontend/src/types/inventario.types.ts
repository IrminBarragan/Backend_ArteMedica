export interface ProductoRequestDTO {
  nombre: string;
  presentacion: string;
  codigoBarras?: string;
  esControlado: boolean;
  categoriaId: number;
  precioVenta: number;
  precioCompra: number;
  stockMinimo: number;
}

export interface ProductoResponseDTO {
  id: number;
  nombre: string;
  presentacion: string;
  codigoBarras: string | null;
  esControlado: boolean;
  categoriaId: number;
  categoriaNombre: string;
  precioVenta: number;
  precioCompra: number;
  stockMinimo: number;
  stockActual: number;
  activo: boolean;
}

export interface CodigoEquivalenteRequestDTO {
  productoId: number;
  codigoBarras: string;
}

export interface CodigoEquivalenteResponseDTO {
  id: number;
  productoId: number;
  productoNombre: string;
  codigoBarras: string;
  activo: boolean;
  createdAt: string;
  createdBy: string;
}

// No existe LoteRequestDTO: los lotes solo se generan implicitamente al registrar una compra (ver compras.types.ts).
export interface LoteResponseDTO {
  id: number;
  numeroLote: string;
  productoId: number;
  productoNombre: string;
  proveedorId: number;
  proveedorNombre: string;
  fechaCaducidad: string;
  costoCompra: number;
  cantidadInicial: number;
  existenciaActual: number;
  activo: boolean;
}

// Enums del kardex (MovimientoInventario). Ningun endpoint los expone todavia (ver docs/MODELOS.md del backend).
export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'MERMA';
export type OrigenMovimiento = 'COMPRA' | 'SOLICITUD' | 'MANUAL';
