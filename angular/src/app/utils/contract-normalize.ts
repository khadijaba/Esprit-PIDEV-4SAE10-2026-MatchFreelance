import { Contract, ContractStatus } from '../models/contract.model';

/** Coerce status from JSON (string, enum name, odd shapes) to a known ContractStatus. */
export function normalizeContractStatus(raw: unknown): ContractStatus {
  if (raw == null) return 'DRAFT';
  if (typeof raw === 'string') {
    const u = raw.trim().toUpperCase();
    if (u === 'DRAFT' || u === 'ACTIVE' || u === 'COMPLETED' || u === 'CANCELLED') return u;
    return u as ContractStatus;
  }
  if (typeof raw === 'object' && raw !== null && 'name' in raw) {
    return normalizeContractStatus((raw as { name: unknown }).name);
  }
  return String(raw).trim().toUpperCase() as ContractStatus;
}

export function normalizeContractFromApi(c: Contract): Contract {
  return {
    ...c,
    id: Number(c.id),
    projectId: Number(c.projectId),
    freelancerId: Number(c.freelancerId),
    clientId: Number(c.clientId),
    status: normalizeContractStatus(c.status as unknown),
  };
}
