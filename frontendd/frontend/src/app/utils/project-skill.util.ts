import { Project } from '../models/project.model';
import { filterToTechnicalSkills } from './skill-taxonomy.util';

/** Découpe une chaîne en compétences individuelles (virgule, point-virgule, "et", etc.). */
function splitSkillString(s: string): string[] {
  return s
    .split(/[,;\n•\-–—]|\s+et\s+|\s+and\s+/i)
    .map((part) => part.trim())
    .filter((part) => part.length >= 2);
}

/**
 * Normalise requiredSkills (backend peut renvoyer string[] | string | JSON string).
 * Chaque compétence requise est un élément distinct : "Java, Angular" → ["Java", "Angular"].
 * Utilisé par Talent Matching (Top 5) et Skill Gap (compatibilité freelancer).
 */
export function normalizeRequiredSkills(requiredSkills: string[] | string | undefined): string[] {
  if (!requiredSkills) return [];
  let rawList: string[];
  if (Array.isArray(requiredSkills)) {
    rawList = requiredSkills.map((s) => (typeof s === 'string' ? s : String(s)).trim()).filter(Boolean);
  } else {
    const raw = String(requiredSkills).trim();
    if (!raw) return [];
    if (raw.startsWith('[')) {
      try {
        const parsed = JSON.parse(raw) as unknown;
        rawList = Array.isArray(parsed)
          ? parsed.map((s) => String(s).trim()).filter(Boolean)
          : splitSkillString(raw);
      } catch {
        rawList = splitSkillString(raw);
      }
    } else {
      rawList = splitSkillString(raw);
    }
  }
  const flattened: string[] = [];
  for (const item of rawList) {
    const parts = splitSkillString(item);
    if (parts.length > 1) flattened.push(...parts);
    else if (item.length >= 2) flattened.push(item);
  }
  return [...new Set(flattened)];
}

/**
 * Extrait les compétences requises depuis la description du projet.
 * Cherche des sections du type "Compétences requises : X, Y, Z" ou "Compétences : ..." / "Skills : ...".
 * Fallback : toute ligne contenant ":" suivie d'une liste séparée par des virgules.
 */
export function extractRequiredSkillsFromDescription(description: string | undefined): string[] {
  if (!description || typeof description !== 'string') return [];
  const text = description.trim();
  if (!text) return [];

  const patterns: RegExp[] = [
    /(?:compétences\s+requises?|skills?\s+requis(?:es)?|required\s+skills?)\s*[:\-]\s*([^\n]+)/i,
    /(?:compétences|skills?)\s*[:\-]\s*([^\n]+)/i,
    /(?:technologies?|tech|langages?)\s*[:\-]\s*([^\n]+)/i,
    /(?:réquis?|prérequis?)\s*[:\-]\s*([^\n]+)/i,
  ];

  let block = '';
  for (const re of patterns) {
    const match = text.match(re);
    if (match && match[1]) {
      block = match[1].trim();
      break;
    }
  }

  if (!block) {
    const lines = text.split(/\n/).map((l) => l.trim()).filter(Boolean);
    for (const line of lines) {
      const colonIdx = line.search(/\s*[:\-]\s*/);
      if (colonIdx > 0 && line.includes(',')) {
        const afterColon = line.slice(colonIdx + 1).replace(/^[\s\-:]+/, '').trim();
        if (afterColon.length >= 3) {
          block = afterColon;
          break;
        }
      }
      if (line.includes(',') && line.length >= 5 && line.length <= 200) {
        const tokens = line.split(/[,;]|\s+et\s+|\s+and\s+/i).map((s) => s.trim()).filter((s) => s.length >= 2 && s.length <= 50);
        if (tokens.length >= 1 && tokens.length <= 25) block = line;
        if (block) break;
      }
    }
  }

  if (!block) return [];

  const raw = block
    .split(/[,;\n•\-–—]|\s+et\s+|\s+and\s+/i)
    .map((s) => s.trim())
    .filter((s) => s.length >= 2 && s.length <= 50);
  return filterToTechnicalSkills([...new Set(raw)]);
}

/** Projet avec requiredSkills : d’abord champ dédié, sinon extrait depuis la description. */
export function normalizeProjectRequiredSkills(project: Project): Project {
  let required = normalizeRequiredSkills(project.requiredSkills);
  if (required.length === 0 && project.description) {
    required = extractRequiredSkillsFromDescription(project.description);
  }
  required = filterToTechnicalSkills(required);
  return { ...project, requiredSkills: required.length ? required : undefined };
}

const MIN_TOKEN_LENGTH = 2;
/** Longueur min pour accepter un match par sous-chaîne (évite "va" → "Java"). */
const MIN_SUBSTRING_LENGTH = 3;

/**
 * Matching intelligent : une compétence requise R matche si
 * - match exact avec une compétence du freelancer, ou
 * - R contient une compétence du freelancer (ou l'inverse) avec au moins MIN_SUBSTRING_LENGTH caractères, ou
 * - au moins un mot significatif de R matche une compétence (ex: "Java Spring Boot" → java, spring, boot).
 */
export function skillMatchesRequired(
  requiredNorm: string,
  freelancerNormalized: string[]
): boolean {
  if (!requiredNorm || !freelancerNormalized.length) return false;
  const r = requiredNorm.trim().toLowerCase().replace(/_/g, ' ');
  const set = new Set(freelancerNormalized.map((f) => f.trim().toLowerCase().replace(/_/g, ' ')).filter(Boolean));

  for (const f of set) {
    if (!f || f.length < MIN_TOKEN_LENGTH) continue;
    if (r === f) return true;
    if ((r.includes(f) || f.includes(r)) && (r.length >= MIN_SUBSTRING_LENGTH && f.length >= MIN_SUBSTRING_LENGTH)) return true;
  }

  const words = r.split(/\s+|,/).map((w) => w.trim().toLowerCase()).filter((w) => w.length >= MIN_TOKEN_LENGTH);
  for (const word of words) {
    if (word.length < MIN_SUBSTRING_LENGTH) continue;
    for (const f of set) {
      if (!f || f.length < MIN_TOKEN_LENGTH) continue;
      if (word === f) return true;
      if ((f.includes(word) || word.includes(f)) && (word.length >= MIN_SUBSTRING_LENGTH && f.length >= MIN_SUBSTRING_LENGTH)) return true;
    }
  }
  return false;
}

/** Calcule le pourcentage de compétences requises couvertes (0–100). */
export function computeCompatibilityPercent(
  requiredSkills: string[],
  freelancerNormalized: string[]
): number {
  if (!requiredSkills.length) return 100;
  const normalized = requiredSkills.map((s) => s.trim().toLowerCase().replace(/_/g, ' ')).filter(Boolean);
  if (!normalized.length) return 100;
  let matched = 0;
  for (const r of normalized) {
    if (skillMatchesRequired(r, freelancerNormalized)) matched++;
  }
  return Math.round((matched / normalized.length) * 100);
}
