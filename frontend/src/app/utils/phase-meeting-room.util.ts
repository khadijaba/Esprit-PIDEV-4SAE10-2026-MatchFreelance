/** Base Jitsi (meet.jit.si public ou instance auto-hébergée). */
export const JITSI_MEET_BASE = 'https://meet.jit.si';

/** Nom de salle déterministe : même meeting = même salle pour owner et freelancer. */
export function phaseMeetingRoomName(projectId: number, phaseId: number, meetingId: number): string {
  return `matchfreelance-p${projectId}-ph${phaseId}-m${meetingId}`;
}

export function phaseMeetingJitsiUrl(projectId: number, phaseId: number, meetingId: number): string {
  const room = phaseMeetingRoomName(projectId, phaseId, meetingId);
  return `${JITSI_MEET_BASE}/${encodeURIComponent(room)}#config.prejoinPageEnabled=false`;
}
