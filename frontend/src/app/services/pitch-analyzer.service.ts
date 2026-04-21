export interface PitchJob {
  id: number;
  title: string;
  company: string;
  budget: { min: number; max: number };
  duration: number;
  skills: string[];
  level: string;
  description: string;
  /** Optional: key requirements from the job (improves Technical + Problem scoring). */
  keyRequirements?: string[];
  /** Optional: dealbreaker phrases (triggers Dealbreaker Check dimension). */
  dealbreakers?: string[];
  /** Optional: nice-to-have skills (small bonus when mentioned). */
  niceToHave?: string[];
}

export interface PitchDimension {
  name: string;
  score: number;
  explanation: string;
  weight: number;
}

export interface PitchAnalysisResult {
  overallScore: number;
  verdict: string;
  dimensions: PitchDimension[];
  keyMatches: string[];
  keyGaps: string[];
  redFlags?: string[];
  /** Single summary for backward compatibility. */
  improvement: string;
  /** Actionable list (preferred for UI). */
  improvements: string[];
  hiringLikelihood: string;
  toneAnalysis: string;
  /** Optional bid suggestion (when job has budget). */
  recommendedBid?: number;
  /** Optional win probability 0–100 (when job has budget). */
  winProbability?: number;
  skillsMatched: string[];
  skillsMissing: string[];
  wordCount: number;
}

function tokenize(text: string): string[] {
  return (text || '')
    .toLowerCase()
    .replace(/[^\w\s]/g, ' ')
    .split(/\s+/)
    .filter(Boolean);
}

const STOP_WORDS = new Set([
  'the', 'and', 'for', 'are', 'but', 'not', 'you', 'all', 'can', 'had', 'her', 'was', 'one', 'our', 'out', 'has', 'his', 'how', 'its', 'may', 'new', 'now', 'old', 'see', 'way', 'who', 'did', 'get', 'got', 'let', 'put', 'say', 'she', 'too', 'use',
  'your', 'this', 'that', 'with', 'from', 'have', 'been', 'will', 'would', 'could', 'should', 'them', 'they', 'what', 'when', 'where', 'which', 'while', 'than', 'then', 'just', 'only', 'also', 'more', 'some', 'very', 'into', 'over', 'such',
]);

/** Tech/skill terms to extract from project (title + description) for matching. */
const TECH_SKILL_PATTERN = /\b(react|angular|vue|node|node\.?js|java|python|typescript|javascript|php|django|spring|sql|mysql|postgresql|mongodb|redis|aws|docker|kubernetes|api|rest|graphql|frontend|backend|fullstack|full.?stack|mobile|flutter|react.?native|redux|zustand|next\.?js|express|nest|tailwind|sass|html|css|figma|swagger|websocket|stripe|firebase|terraform|ci\/cd|jenkins|github|gitlab|azure|gcp|nosql|elasticsearch|microservices|agile|scrum|jira)\b/gi;

/** Experience/credibility markers in the pitch. */
const EXP_MARKERS = [
  'experience', 'years', 'built', 'shipped', 'worked', 'developed', 'implemented', 'led', 'senior', 'expert',
  'production', 'previous', 'portfolio', 'client', 'architected', 'launched', 'delivered', 'team', 'certified',
  'professional', 'extensive', 'proven', 'track record', 'successfully', 'completed', 'handled', 'managed',
  'designed', 'engineered', 'deployed', 'maintained', 'scaled', 'optimized', 'integrated', 'automated',
  'freelance', 'freelancer', 'contractor', 'consultant', 'agency', 'startup', 'enterprise', 'b2b', 'b2c',
];

/** Value proposition / fit markers. */
const VALUE_MARKERS = [
  'fit', 'right choice', 'best', 'why i', 'perfect', 'ideal', 'trust', 'reliable', 'deliver', 'commit',
  'dedicated', 'match', 'align', 'excited', 'passionate', 'interested', 'available', 'ready', 'can start',
  'understand', 'familiar', 'experienced in', 'specialize', 'focus', 'expertise', 'strong fit', 'great fit',
  'looking forward', 'happy to', 'glad to', 'would love', 'ideal candidate', 'suitable', 'well-suited',
];

/** Specificity markers (shows they reference concrete details). */
const SPECIFICITY_MARKERS = [
  'specifically', 'in particular', 'for example', 'such as', 'including', 'using', 'with', 'via', 'through',
  'by implementing', 'by building', 'the reason', 'because', 'therefore', 'which means', 'this allows',
];

/** Confidence-positive language. */
const CONFIDENCE_POSITIVE = [
  'confident', 'certain', 'guarantee', 'ensure', 'proven', 'expert', 'specialist', 'deep knowledge', 'extensive',
  'proficient', 'strong', 'solid', 'hands-on', 'passionate', 'dedicated', 'committed', 'deliver', 'on time', 'quality',
];

/** Confidence-negative / uncertain language. */
const CONFIDENCE_NEGATIVE = [
  'maybe', 'possibly', 'i think', 'i guess', 'not sure', 'might', 'could try', 'learning', 'beginner', 'junior', 'limited', 'basic knowledge',
];

/** Vague generic phrases that weaken the pitch. */
const VAGUE_PHRASES = [
  'i am a developer', 'i can do this', 'i am interested', 'please hire me', 'i will do my best', 'i am available',
  'contact me', 'i am good at', 'i know everything', 'fast learner', 'hard worker', 'team player', 'i can handle', 'no problem', 'easy task',
];

/** Technical depth terms (precise language). */
const TECHNICAL_DEPTH_TERMS = [
  'api', 'rest', 'graphql', 'sql', 'nosql', 'cache', 'async', 'deploy', 'docker', 'container', 'server', 'client',
  'database', 'query', 'index', 'optimize', 'refactor', 'architect', 'pipeline', 'auth', 'token', 'webhook', 'socket',
  'stream', 'batch', 'queue', 'microservice', 'monolith', 'scale', 'latency', 'throughput',
];

/** Problem/solution language. */
const PROBLEM_WORDS = [
  'challenge', 'solution', 'problem', 'need', 'require', 'goal', 'objective', 'result', 'outcome', 'deliver',
  'achieve', 'ensure', 'improve', 'optimize', 'scalab', 'perform', 'secur', 'reliab', 'maintainab',
];

/** Approach / plan language. */
const APPROACH_WORDS = [
  'approach', 'would', 'plan', 'strategy', 'first', 'then', 'start by', 'begin with', 'structure', 'architect', 'design', 'setup', 'configure',
];

/** Portfolio / proof signals. */
const PORTFOLIO_WORDS = [
  'github', 'portfolio', 'project', 'demo', 'link', 'example', 'case study', 'built for', 'worked with', 'delivered to',
  'client', 'production', 'live', 'deployed', 'published',
];

const LEVEL_MAP: Record<string, number> = { Junior: 1, Mid: 2, Senior: 3, Expert: 4 };

/** Dimension weights (align with JSX: Technical 28%, Problem 22%, Experience 20%, Comm 15%, Value 10%, Dealbreaker 5%). */
const DIMENSION_WEIGHTS = {
  technicalRelevance: 0.28,
  problemUnderstanding: 0.22,
  experienceSignals: 0.20,
  communicationQuality: 0.15,
  valueProposition: 0.10,
  dealBreakerCheck: 0.05,
};

function countMatches(text: string, keywords: string[]): number {
  const lower = (text || '').toLowerCase();
  return keywords.filter((kw) => lower.includes(kw.toLowerCase())).length;
}

function getWordCount(text: string): number {
  return (text || '').trim().split(/\s+/).filter(Boolean).length;
}

function getSentenceCount(text: string): number {
  return (text || '').split(/[.!?]+/).filter((s) => s.trim().length > 10).length;
}

function getAverageSentenceLength(text: string): number {
  const sentences = (text || '').split(/[.!?]+/).filter((s) => s.trim().length > 5);
  if (!sentences.length) return 0;
  const total = sentences.reduce((sum, s) => sum + s.trim().split(/\s+/).length, 0);
  return total / sentences.length;
}

function extractKeywords(text: string, minLen = 3): string[] {
  const words = tokenize(text);
  return [...new Set(words.filter((w) => w.length >= minLen && !STOP_WORDS.has(w)))];
}

/** Build a PitchJob from a project. Skills and keywords from title + description for better matching. */
export function projectToPitchJob(project: {
  id: number;
  title: string;
  description: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  clientName?: string;
}): PitchJob {
  const desc = project.description || '';
  const title = project.title || '';
  const combined = `${title} ${desc}`;
  const techMatches = [...combined.matchAll(TECH_SKILL_PATTERN)].map((m) => m[1].toLowerCase().replace(/\./g, ''));
  const skillsFromText = [...new Set(techMatches)];
  const keywordSkills = extractKeywords(combined, 4).filter((w) => w.length <= 20).slice(0, 16);
  const skills = skillsFromText.length > 0 ? skillsFromText : [...new Set(keywordSkills)].slice(0, 12);
  return {
    id: project.id,
    title: project.title,
    company: project.clientName || 'Client',
    budget: { min: project.minBudget, max: project.maxBudget },
    duration: project.duration,
    skills: skills.length > 0 ? skills : ['project', 'requirements'],
    level: 'Mid',
    description: desc,
  };
}

function scoreTechnicalRelevance(
  pitch: string,
  job: PitchJob
): { score: number; explanation: string; matched: string[]; missing: string[] } {
  const pitchLower = pitch.toLowerCase();
  const pitchTokenSet = new Set(tokenize(pitch));
  const jobSkills = (job.skills || []).map((s) => s.toLowerCase());
  const mentionedSkills = jobSkills.filter((s) => pitchLower.includes(s));
  const missing = jobSkills.filter((s) => !pitchLower.includes(s));
  const skillScore = jobSkills.length ? (mentionedSkills.length / jobSkills.length) * 100 : 50;

  let reqScore = 50;
  const keyReqs = job.keyRequirements || [];
  if (keyReqs.length) {
    const reqWords = keyReqs.flatMap((r) => tokenize(r)).filter((w) => w.length > 4);
    const covered = reqWords.filter((w) => pitchTokenSet.has(w)).length;
    const total = reqWords.filter((w) => w.length > 4).length;
    reqScore = total ? Math.min(150, (covered / total) * 150) : 50;
  }

  const techDepth = countMatches(pitch, TECHNICAL_DEPTH_TERMS);
  const techDepthScore = Math.min(100, (techDepth / 8) * 100);

  let bonusScore = 0;
  const niceToHave = job.niceToHave || [];
  if (niceToHave.length) {
    const niceMatches = niceToHave.filter((n) => pitchLower.includes(n.toLowerCase())).length;
    bonusScore = (niceMatches / niceToHave.length) * 20;
  }

  const descAndTitle = `${job.description || ''} ${job.title || ''}`;
  const jobKeywords = [...jobSkills, ...extractKeywords(descAndTitle).slice(0, 35)];
  const uniqJob = [...new Set(jobKeywords)];
  const keywordRatio = uniqJob.length ? uniqJob.filter((kw) => pitchLower.includes(kw)).length / uniqJob.length : 0;
  const keywordScore = keywordRatio * 100;

  const score = Math.min(100, Math.round(skillScore * 0.40 + reqScore * 0.30 + techDepthScore * 0.15 + keywordScore * 0.10 + bonusScore * 0.05));
  const explanation =
    mentionedSkills.length >= jobSkills.length / 2
      ? `Your pitch mentions ${mentionedSkills.join(', ')} and aligns well with the required tech stack.`
      : mentionedSkills.length > 0
        ? `You mention ${mentionedSkills.join(', ')} but could add ${missing.slice(0, 3).join(', ')}.`
        : `Reference required skills (e.g. ${jobSkills.slice(0, 4).join(', ')}) and how you would use them.`;
  return { score, explanation, matched: mentionedSkills, missing };
}

function scoreProblemUnderstanding(pitch: string, job: PitchJob): { score: number; explanation: string } {
  const pitchLower = pitch.toLowerCase();
  const pitchTokenSet = new Set(tokenize(pitch));
  const descWords = tokenize(job.description || '').filter((w) => w.length > 5);
  const domainOverlap = descWords.filter((w) => pitchTokenSet.has(w)).length;
  const domainScore = Math.min(100, (domainOverlap / Math.max(descWords.length * 0.15, 1)) * 100);
  const specificityScore = Math.min(100, countMatches(pitch, SPECIFICITY_MARKERS) * 12);
  const problemScore = Math.min(100, countMatches(pitch, PROBLEM_WORDS) * 10);
  const approachScore = Math.min(100, countMatches(pitch, APPROACH_WORDS) * 12);
  const score = Math.min(100, Math.round(domainScore * 0.35 + specificityScore * 0.25 + problemScore * 0.20 + approachScore * 0.20));
  const explanation =
    score >= 60
      ? `You address project-specific concepts and show you read the brief.`
      : score >= 35
        ? `Add more references to the client's exact problem and your approach.`
        : `Echo the client's language and goals so they see you understand the scope.`;
  return { score, explanation };
}

function scoreExperienceSignals(
  pitch: string,
  job: PitchJob
): { score: number; explanation: string; wordCount: number; numbers: number } {
  const pitchLower = pitch.toLowerCase();
  const wordCount = getWordCount(pitch);
  const expCount = countMatches(pitch, EXP_MARKERS);
  const expScore = Math.min(100, (expCount / 6) * 100);
  const numbersMatch = pitch.match(/\d+(\+|k|%|x|\s*years|\s*months|\s*clients|\s*projects|\s*users|\s*ms)?/gi) || [];
  const numbers = numbersMatch.length;
  const metricsScore = Math.min(100, numbers * 12);
  const portfolioScore = Math.min(100, countMatches(pitch, PORTFOLIO_WORDS) * 10);
  const lengthScore = wordCount < 50 ? 20 : wordCount < 100 ? 50 : wordCount < 200 ? 75 : wordCount < 400 ? 95 : 85;
  const jobLevel = LEVEL_MAP[job.level] ?? 2;
  const seniorWords = ['led', 'architected', 'mentored', 'designed system', 'owned', 'responsible', 'senior', 'lead', 'principal', 'years of experience'];
  const juniorWords = ['learning', 'studying', 'beginner', 'first job', 'recent graduate', 'just started', 'entry level'];
  const seniorCount = countMatches(pitch, seniorWords);
  const juniorCount = countMatches(pitch, juniorWords);
  const levelPenalty = juniorCount > 0 && jobLevel >= 3 ? -20 : 0;
  const levelBonus = seniorCount > 0 && jobLevel >= 3 ? 10 : 0;
  const hasOutcome = /\b(successfully|delivered|launched|shipped|reduced|increased|improved|saved|built)\b/i.test(pitch);
  const score = Math.min(
    100,
    Math.max(
      0,
      Math.round(expScore * 0.30 + metricsScore * 0.25 + portfolioScore * 0.25 + lengthScore * 0.20) + levelPenalty + levelBonus + (hasOutcome ? 10 : 0)
    )
  );
  const explanation =
    score >= 70
      ? `Concrete experience signals, numbers, and context.`
      : score >= 45
        ? `Add more specific experience (years, projects, outcomes).`
        : `Include clear evidence: years, similar projects, or measurable results.`;
  return { score, explanation, wordCount, numbers };
}

function scoreCommunicationQuality(
  pitch: string
): { score: number; explanation: string; vagueCount: number; posCount: number; negCount: number } {
  const wordCount = getWordCount(pitch);
  const avgLen = getAverageSentenceLength(pitch);
  const posCount = countMatches(pitch, CONFIDENCE_POSITIVE);
  const negCount = countMatches(pitch, CONFIDENCE_NEGATIVE);
  const confidenceScore = Math.min(100, Math.max(0, posCount * 12 - negCount * 15));
  const vagueCount = countMatches(pitch, VAGUE_PHRASES);
  const vaguePenalty = vagueCount * 10;
  const structureScore = avgLen >= 10 && avgLen <= 25 ? 90 : avgLen < 10 ? 50 : 65;
  const paragraphs = pitch.split(/\n+/).filter((p) => p.trim().length > 20).length;
  const paragraphScore = paragraphs >= 2 ? 85 : paragraphs === 1 ? 60 : 40;
  const allCapsWords = (pitch.match(/\b[A-Z]{3,}\b/g) || []).filter(
    (w) =>
      !['API', 'SQL', 'AWS', 'CSS', 'HTML', 'REST', 'SDK', 'MVP', 'SaaS', 'CI', 'CD', 'URL', 'HTTP', 'HTTPS', 'JSON', 'JWT', 'ORM', 'OOP', 'TDD', 'MVC', 'SPA', 'PWA', 'SSR', 'CDN', 'VPC', 'IAM', 'EKS', 'RDS', 'S3', 'EC2', 'SQS', 'SNS', 'FCM'].includes(w)
  ).length;
  const formalityScore = Math.max(40, 100 - allCapsWords * 10);
  const exclamations = (pitch.match(/!/g) || []).length;
  const questionMarks = (pitch.match(/\?/g) || []).length;
  const exclamScore = exclamations <= 2 ? 15 : exclamations <= 5 ? 5 : -10;
  const qScore = questionMarks <= 1 ? 8 : -5;
  const raw =
    Math.round(confidenceScore * 0.30 + structureScore * 0.25 + paragraphScore * 0.25 + formalityScore * 0.20) + exclamScore + qScore - vaguePenalty;
  const score = Math.min(100, Math.max(0, raw));
  const explanation =
    wordCount >= 70 && exclamations <= 2 && vagueCount === 0
      ? `Clear structure and professional tone.`
      : vagueCount > 1
        ? `Replace generic phrases with specific examples.`
        : wordCount < 40
          ? `A longer, well-structured pitch (2–3 paragraphs) usually performs better.`
          : `Consider short paragraphs and confident, concrete language.`;
  return { score, explanation, vagueCount, posCount, negCount };
}

function scoreValueProposition(
  pitch: string,
  job: PitchJob
): { score: number; explanation: string; companyMentioned: boolean; titleMentioned: boolean } {
  const pitchLower = pitch.toLowerCase();
  const whyMeScore = Math.min(100, countMatches(pitch, VALUE_MARKERS) * 14);
  const timelineWords = ['deadline', 'on time', 'deliver', 'schedule', 'timeline', 'within', 'days', 'weeks', 'milestone', 'phase', 'sprint'];
  const timelineScore = Math.min(100, countMatches(pitch, timelineWords) * 12);
  const commWords = ['update', 'report', 'communicate', 'available', 'response', 'standup', 'progress', 'transparent', 'daily', 'weekly'];
  const commScore = Math.min(100, countMatches(pitch, commWords) * 14);
  const companyMentioned = job.company ? pitchLower.includes(job.company.toLowerCase()) : false;
  const titleWords = (job.title || '').toLowerCase().split(/\s+/).filter((w) => w.length > 3);
  const titleMentioned = titleWords.some((w) => pitchLower.includes(w));
  const personalScore = (companyMentioned ? 40 : 0) + (titleMentioned ? 30 : 0);
  const score = Math.min(100, Math.round(whyMeScore * 0.35 + timelineScore * 0.25 + commScore * 0.20 + Math.min(personalScore, 100) * 0.20));
  const explanation =
    score >= 60
      ? `You articulate why you are a good fit.`
      : !companyMentioned && !titleMentioned
        ? `Mention ${job.company || 'the company'} or the role to personalize the pitch.`
        : `Add one clear sentence on why you are the right choice for this project.`;
  return { score, explanation, companyMentioned, titleMentioned };
}

function checkDealbreakers(
  pitch: string,
  job: PitchJob
): { score: number; addressed: string[]; hits: string[] } {
  const dealbreakers = job.dealbreakers || [];
  if (!dealbreakers.length) return { score: 50, addressed: [], hits: [] };
  const pitchLower = pitch.toLowerCase();
  const pitchTokenSet = new Set(tokenize(pitch));
  const hits = dealbreakers.filter((db) => {
    const dbWords = tokenize(db).filter((w) => w.length > 4);
    return dbWords.some((w) => pitchLower.includes(w));
  });
  const addressed = dealbreakers.filter((db) => {
    const dbWords = tokenize(db).filter((w) => w.length > 4);
    const overlap = dbWords.filter((w) => pitchTokenSet.has(w)).length;
    return overlap >= Math.ceil(dbWords.length * 0.5);
  });
  const score = addressed.length > 0 ? Math.min(100, 60 + addressed.length * 15) : 50;
  return { score, addressed, hits };
}

export function analyzePitch(job: PitchJob, pitch: string): PitchAnalysisResult {
  const p = (pitch || '').trim();
  const pitchLower = p.toLowerCase();
  const pitchWordCount = getWordCount(p);

  const tech = scoreTechnicalRelevance(p, job);
  const prob = scoreProblemUnderstanding(p, job);
  const exp = scoreExperienceSignals(p, job);
  const comm = scoreCommunicationQuality(p);
  const val = scoreValueProposition(p, job);
  const db = checkDealbreakers(p, job);

  const clampedOverall = Math.max(
    0,
    Math.min(
      100,
      Math.round(
        tech.score * DIMENSION_WEIGHTS.technicalRelevance +
          prob.score * DIMENSION_WEIGHTS.problemUnderstanding +
          exp.score * DIMENSION_WEIGHTS.experienceSignals +
          comm.score * DIMENSION_WEIGHTS.communicationQuality +
          val.score * DIMENSION_WEIGHTS.valueProposition +
          db.score * DIMENSION_WEIGHTS.dealBreakerCheck
      )
    )
  );

  const verdict =
    clampedOverall >= 78
      ? 'Strong alignment with the project; your pitch addresses skills, context, and fit well.'
      : clampedOverall >= 58
        ? 'Good base; a few targeted additions could improve your match.'
        : clampedOverall >= 38
          ? 'Some overlap exists but the pitch could be much more specific to this project.'
          : 'The pitch is too generic or short; tailor it to the project and add evidence.';

  const keyMatches: string[] = [];
  if (tech.matched.length)
    keyMatches.push(`Mentions ${tech.matched.length} required skills: ${tech.matched.slice(0, 4).join(', ')}`);
  if (exp.numbers > 2) keyMatches.push(`Uses ${exp.numbers} quantified data points — signals credibility`);
  if (comm.posCount > 2) keyMatches.push('Uses confident, professional language');
  if (val.companyMentioned && job.company) keyMatches.push(`Personalizes by mentioning ${job.company}`);
  if (db.addressed.length) keyMatches.push(`Addresses ${db.addressed.length} potential dealbreaker(s) proactively`);
  if (exp.wordCount >= 150) keyMatches.push(`Substantial length (${exp.wordCount} words) shows effort`);
  if (keyMatches.length === 0) keyMatches.push('Consider highlighting at least one required skill or keyword.');

  const keyGaps: string[] = tech.missing.length
    ? tech.missing.slice(0, 4).map((s) => `Does not mention: ${s}`)
    : ['No major skill gaps detected.'];
  if (exp.numbers < 2) keyGaps.push('No quantified achievements or metrics — add numbers.');
  if (!val.companyMentioned && !val.titleMentioned) keyGaps.push('Pitch is not personalized to this job or company.');
  if (comm.vagueCount > 1) keyGaps.push(`Contains ${comm.vagueCount} vague phrase(s) that weaken the pitch.`);
  if (exp.wordCount < 100) keyGaps.push('Pitch is too short — add more detail.');
  if (comm.negCount > 0) keyGaps.push('Uses uncertain language (maybe, might) which reduces confidence.');

  const redFlags: string[] = [];
  if (comm.vagueCount > 3) redFlags.push('Excessive generic phrases — pitch reads as a template.');
  if (exp.wordCount < 50) redFlags.push('Critically short pitch — signals low effort.');
  if (comm.negCount > 2) redFlags.push('Multiple instances of uncertain language.');
  if (tech.score < 25) redFlags.push('Very low technical overlap — may not have the required stack.');
  if (clampedOverall < 30) redFlags.push('Overall very weak match — significant gaps.');

  const improvements: string[] = [];
  if (tech.missing.length > 2)
    improvements.push(`Explicitly mention experience with ${tech.missing.slice(0, 3).join(', ')}.`);
  if (exp.numbers < 3)
    improvements.push(
      "Add specific numbers: years of experience, projects, team sizes, or outcomes (e.g. 'reduced load time by 40%')."
    );
  if (!val.companyMentioned && job.company)
    improvements.push(`Mention ${job.company} by name and reference a detail from the project description.`);
  if (comm.vagueCount > 0)
    improvements.push("Replace generic phrases like 'I can do this' or 'fast learner' with specific examples.");
  if (exp.wordCount < 150)
    improvements.push('Expand to at least 150–250 words — detail your approach, past work, and why you fit.');
  if (improvements.length === 0) improvements.push('Add one specific deliverable or timeline to stand out.');

  const hiringLikelihood =
    clampedOverall >= 80 ? 'Very High' : clampedOverall >= 65 ? 'High' : clampedOverall >= 45 ? 'Medium' : 'Low';
  let toneAnalysis = 'Balanced';
  if (comm.negCount > 2) toneAnalysis = 'Underconfident';
  else if (comm.posCount > 5 && comm.negCount === 0) toneAnalysis = 'Overconfident';
  else if (comm.posCount > 2) toneAnalysis = 'Professional';
  else if (comm.vagueCount > 2) toneAnalysis = 'Casual';
  const questionMarks = (p.match(/\?/g) || []).length;
  if (questionMarks >= 3) toneAnalysis = 'Inquisitive';

  const budgetMid = job.budget?.min != null && job.budget?.max != null ? (job.budget.min + job.budget.max) / 2 : 0;
  const recommendedBid =
    budgetMid > 0 ? Math.round(budgetMid * (0.85 + (clampedOverall / 100) * 0.25)) : undefined;
  const winProbability =
    budgetMid > 0 ? Math.round(Math.max(5, Math.min(95, (clampedOverall / 100) * 85))) : undefined;

  const dimensions: PitchDimension[] = [
    { name: 'Technical Relevance', score: tech.score, explanation: tech.explanation, weight: DIMENSION_WEIGHTS.technicalRelevance * 100 },
    { name: 'Problem Understanding', score: prob.score, explanation: prob.explanation, weight: DIMENSION_WEIGHTS.problemUnderstanding * 100 },
    { name: 'Experience Signals', score: exp.score, explanation: exp.explanation, weight: DIMENSION_WEIGHTS.experienceSignals * 100 },
    { name: 'Communication Quality', score: comm.score, explanation: comm.explanation, weight: DIMENSION_WEIGHTS.communicationQuality * 100 },
    { name: 'Value Proposition', score: val.score, explanation: val.explanation, weight: DIMENSION_WEIGHTS.valueProposition * 100 },
  ];
  if (job.dealbreakers?.length) {
    dimensions.push({
      name: 'Dealbreaker Check',
      score: db.score,
      explanation: db.addressed.length ? `You addressed ${db.addressed.length} dealbreaker(s).` : 'Consider addressing the listed dealbreakers.',
      weight: DIMENSION_WEIGHTS.dealBreakerCheck * 100,
    });
  }

  return {
    overallScore: clampedOverall,
    verdict,
    dimensions,
    keyMatches,
    keyGaps,
    redFlags: redFlags.length ? redFlags : undefined,
    improvement: improvements.join(' '),
    improvements,
    hiringLikelihood,
    toneAnalysis,
    recommendedBid,
    winProbability,
    skillsMatched: tech.matched,
    skillsMissing: tech.missing,
    wordCount: exp.wordCount,
  };
}

/** Demo jobs aligned with backend project-service seed (projects 1–3). Used for pitch analysis UI. */
export const PITCH_JOBS: PitchJob[] = [
  {
    id: 1,
    title: 'SaaS Analytics Dashboard – React & TypeScript',
    company: 'FreelanceHub Demo',
    budget: { min: 8000, max: 12000 },
    duration: 60,
    skills: ['React', 'TypeScript', 'Node.js', 'PostgreSQL', 'Redux', 'GraphQL'],
    level: 'Senior',
    description: `Build a B2B analytics dashboard with real-time data, charts (D3/Recharts), multi-tenant architecture and role-based access. Required: React, TypeScript, Node.js, PostgreSQL, state management (Redux/Zustand), REST/GraphQL APIs. CI/CD and testing (Jest, Cypress) expected.`,
    keyRequirements: [
      'React + TypeScript production experience',
      'State management (Redux / Zustand)',
      'GraphQL API integration',
      'Performance optimization (virtualization, memoization)',
      'CI/CD pipeline familiarity',
      'Unit and integration testing (Jest, Cypress)',
    ],
    dealbreakers: ['No TypeScript experience', 'Never shipped a production SaaS product', 'No testing experience'],
    niceToHave: ['Figma handoff', 'WebSocket experience', 'Storybook component library'],
  },
  {
    id: 2,
    title: 'FinTech Payment API – Full-Stack',
    company: 'FreelanceHub Demo',
    budget: { min: 10000, max: 15000 },
    duration: 90,
    skills: ['Python', 'Django', 'React', 'AWS', 'Redis', 'PostgreSQL', 'Stripe'],
    level: 'Senior',
    description: `Design and implement the core of our B2B payment processing API: secure flows, webhooks, Stripe integration, admin dashboard. Security is critical: PCI-DSS awareness, encryption, audit logs. Stack: Django REST Framework, React, AWS, Redis. Prior payment or fintech experience required.`,
    keyRequirements: [
      'Django REST Framework API design',
      'Stripe or payment gateway integration',
      'PCI-DSS security awareness',
      'AWS infrastructure (EC2, RDS, SQS)',
      'Redis caching and rate limiting',
      'Idempotent payment flows',
    ],
    dealbreakers: ['No payment systems experience', 'No security/encryption knowledge', 'No AWS experience'],
    niceToHave: ['Financial reconciliation', 'Double-entry bookkeeping', 'SOC2 compliance'],
  },
  {
    id: 3,
    title: 'Logistics & Tracking – Backend API',
    company: 'FreelanceHub Demo',
    budget: { min: 5000, max: 7000 },
    duration: 45,
    skills: ['Node.js', 'Express', 'MongoDB', 'Docker', 'WebSockets', 'Swagger'],
    level: 'Mid',
    description: `RESTful APIs for a logistics platform: real-time shipment status, driver location (WebSockets), route optimization, SMS/email notifications. MongoDB, Node.js, Express, Docker. All APIs documented with Swagger/OpenAPI. Clean code and API versioning required.`,
    keyRequirements: [
      'Node.js + Express REST API design',
      'MongoDB + geospatial queries',
      'WebSocket real-time updates',
      'Docker containerization',
      'Swagger/OpenAPI documentation',
      'Third-party integrations (Twilio, SendGrid)',
    ],
    dealbreakers: ['No MongoDB experience', 'Never built real-time features', 'No Docker knowledge'],
    niceToHave: ['Kafka / RabbitMQ', 'Kubernetes', 'Google Maps API integration'],
  },
];

/** MatchFreelance validation `Project` (single budget) → PitchJob for analyzePitch. */
export function matchfreelanceProjectToPitchJob(project: {
  id: number;
  title: string;
  description: string;
  budget: number;
  duration: number;
  requiredSkills?: string[];
}): PitchJob {
  const b = Number.isFinite(project.budget) && project.budget > 0 ? project.budget : 1;
  const job = projectToPitchJob({
    id: project.id,
    title: project.title,
    description: project.description,
    minBudget: b,
    maxBudget: b,
    duration: project.duration,
    clientName: 'Client',
  });
  if (project.requiredSkills?.length) {
    const extra = project.requiredSkills.map((s) => s.toLowerCase().trim()).filter(Boolean);
    job.skills = [...new Set([...job.skills, ...extra])].slice(0, 16);
  }
  return job;
}
