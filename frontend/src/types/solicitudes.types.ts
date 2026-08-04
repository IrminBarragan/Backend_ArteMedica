export type EstatusSolicitud = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ENTREGADA_PARCIAL' | 'ENTREGADA_COMPLETA';

export interface SolicitudDetalleRequestDTO {
  productoId: number;
  cantidadSolicitada: number;
}

export interface SolicitudRequestDTO {
  areaId: number;
  detalles: SolicitudDetalleRequestDTO[];
}

export interface SolicitudDetalleResponseDTO {
  id: number;
  productoId: number;
  productoNombre: string;
  presentacion: string;
  cantidadSolicitada: number;
  cantidadAutorizada: number | null;
  cantidadEntregada: number;
  loteId: number | null;
  numeroLote: string | null;
}

export interface SolicitudResponseDTO {
  id: number;
  medicoId: number;
  medicoNombre: string;
  areaId: number;
  areaNombre: string;
  fechaSolicitud: string;
  estatus: EstatusSolicitud;
  farmaceuticoNombre: string | null;
  fechaAprobacion: string | null;
  fechaEntrega: string | null;
  motivoRechazo: string | null;
  detalles: SolicitudDetalleResponseDTO[];
  createdAt: string;
}

export interface AprobarSolicitudRequestDTO {
  cantidadesAutorizadasPorProducto: Record<number, number>;
}

export interface RechazarSolicitudRequestDTO {
  motivo: string;
}
