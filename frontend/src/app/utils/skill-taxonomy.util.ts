/**
 * Taxonomie des compétences : liste de compétences techniques reconnues + liste noire (métiers, mots non techniques).
 * Permet de filtrer l'extraction depuis la description ou le champ requiredSkills pour éviter "Médecin", "Patient", etc.
 * Pour un vrai NLP/LLM, brancher un service backend qui appelle un modèle et utiliser sa réponse ici.
 */

const TECHNICAL_SKILLS = new Set([
  'java', 'javascript', 'typescript', 'python', 'php', 'c#', 'csharp', 'c++', 'ruby', 'go', 'golang', 'rust', 'kotlin', 'swift', 'scala',
  'angular', 'react', 'vue', 'vuejs', 'node', 'nodejs', 'express', 'nestjs', 'next', 'nextjs', 'spring', 'spring boot', 'springboot',
  'django', 'flask', 'fastapi', 'laravel', 'symfony', 'dotnet', '.net', 'asp.net',
  'html', 'css', 'sass', 'scss', 'tailwind', 'bootstrap', 'redux', 'rxjs',
  'rest', 'rest api', 'graphql', 'api', 'microservices', 'microservice', 'jwt', 'oauth', 'oauth2',
  'docker', 'kubernetes', 'k8s', 'jenkins', 'git', 'ci/cd', 'devops', 'aws', 'azure', 'gcp', 'linux', 'sql', 'nosql',
  'mongodb', 'mysql', 'postgresql', 'postgres', 'redis', 'elasticsearch',
  'machine learning', 'ml', 'ai', 'tensorflow', 'pytorch', 'data science', 'nlp', 'computer vision',
  'agile', 'scrum', 'jira', 'figma', 'ui', 'ux', 'design', 'cybersécurité', 'cybersecurity', 'security',
  'testing', 'jest', 'junit', 'selenium', 'tdd', 'bdd',
  'git', 'github', 'gitlab', 'bitbucket', 'maven', 'gradle', 'npm', 'yarn', 'webpack', 'vite',
  'architecture', 'clean code', 'design pattern', 'api rest', 'soap', 'websocket', 'grpc',
  'flutter', 'react native', 'xamarin', 'ionic', 'android', 'ios',
  'excel', 'power bi', 'tableau', 'r', 'spark', 'hadoop', 'kafka', 'rabbitmq',
]);

/** Mots / métiers à exclure (non compétences techniques). */
const BLACKLIST = new Set([
  'médecin', 'medecin', 'patient', 'docteur', 'infirmier', 'infirmière', 'admin', 'administrateur',
  'client', 'utilisateur', 'user', 'manager', 'équipe', 'equipe', 'projet', 'project',
  'gestion', 'rôles', 'roles', 'role', 'développeur', 'developpeur', 'freelance', 'freelancer',
  'entreprise', 'société', 'societe', 'application', 'site', 'web', 'système', 'systeme',
  'données', 'donnees', 'data', 'information', 'service', 'solutions', 'solution',
  'travail', 'work', 'job', 'mission', 'description', 'contexte', 'objectif', 'objectifs',
  'année', 'annee', 'années', 'experience', 'expérience', 'années d\'expérience',
  'niveau', 'débutant', 'debutant', 'expert', 'intermédiaire', 'intermediaire',
  'ouvert', 'ouverte', 'terminé', 'termine', 'annulé', 'annule', 'en cours',
]);

function normalizeForMatch(s: string): string {
  return s.trim().toLowerCase().replace(/_/g, ' ').replace(/\s+/g, ' ');
}

/**
 * Retourne true si le terme est considéré comme une compétence technique (présent dans la liste technique
 * ou contient un mot-clé technique), et false s'il est dans la liste noire ou inconnu.
 */
export function isTechnicalSkill(term: string): boolean {
  const n = normalizeForMatch(term);
  if (!n || n.length < 2) return false;
  if (BLACKLIST.has(n)) return false;
  for (const b of BLACKLIST) {
    if (n === b || n.includes(b) || b.length >= 4 && n.includes(b)) return false;
  }
  if (TECHNICAL_SKILLS.has(n)) return true;
  const words = n.split(/\s+/).filter((w) => w.length >= 2);
  for (const w of words) {
    if (TECHNICAL_SKILLS.has(w)) return true;
    for (const t of TECHNICAL_SKILLS) {
      if (t.length >= 3 && (w.includes(t) || t.includes(w))) return true;
    }
  }
  for (const t of TECHNICAL_SKILLS) {
    if (t.length >= 4 && (n.includes(t) || t.includes(n))) return true;
  }
  return false;
}

/**
 * Filtre une liste de compétences pour ne garder que celles reconnues comme techniques.
 * À utiliser après extractRequiredSkillsFromDescription ou sur requiredSkills du projet.
 */
export function filterToTechnicalSkills(skills: string[]): string[] {
  return skills.filter((s) => isTechnicalSkill(s));
}

/**
 * Option : si vous branchez un service NLP/LLM (backend), remplacer l'extraction par un appel API
 * qui renvoie des compétences structurées. Ex. :
 *   POST /api/skills/extract-from-text { "text": "..." } -> { "skills": ["Java", "Spring Boot"] }
 */
