import { useState, useCallback } from "react";

// ─── CONSTANTS ───────────────────────────────────────────────────────────────

const PLATFORM_COMMISSION = 0.10;
const TVA_RATE = 0.19;

// ─── JOBS DATABASE ────────────────────────────────────────────────────────────

const JOBS = [
  {
    id: 1,
    title: "Senior React Developer – SaaS Dashboard",
    company: "TechVision",
    category: "Frontend",
    budget: { min: 8000, max: 12000 },
    duration: 60,
    level: "Senior",
    skills: ["React", "TypeScript", "Node.js", "PostgreSQL", "Redux", "GraphQL"],
    posted: "2h ago",
    applicants: 14,
    description: `We are building a complex SaaS analytics dashboard for B2B clients. The platform must handle real-time data streams, complex chart visualizations using D3 or Recharts, multi-tenant architecture, and role-based access control. It must be highly performant with large datasets (100k+ rows). We need someone who deeply understands React state management (Redux or Zustand), can build reusable component libraries, and has shipped production-grade TypeScript codebases. API integration with REST and GraphQL endpoints is required. The candidate must have experience with CI/CD pipelines, writing unit and integration tests with Jest and Cypress, and performance optimization techniques like memoization, lazy loading, and virtualization. Familiarity with Figma handoff and pixel-perfect implementation is a strong plus.`,
    keyRequirements: [
      "React + TypeScript production experience",
      "State management (Redux / Zustand)",
      "GraphQL API integration",
      "Performance optimization (virtualization, memoization)",
      "CI/CD pipeline familiarity",
      "Unit and integration testing (Jest, Cypress)",
      "Multi-tenant SaaS architecture",
      "Data visualization (D3, Recharts)",
    ],
    dealbreakers: [
      "No TypeScript experience",
      "Never shipped a production SaaS product",
      "No testing experience",
    ],
    niceToHave: ["Figma handoff", "WebSocket experience", "Storybook component library"],
  },
  {
    id: 2,
    title: "Full-Stack Engineer – FinTech Payment Platform",
    company: "PayFlow",
    category: "Full-Stack",
    budget: { min: 10000, max: 15000 },
    duration: 90,
    level: "Senior",
    skills: ["Python", "Django", "React", "AWS", "Redis", "PostgreSQL", "Stripe"],
    posted: "5h ago",
    applicants: 9,
    description: `PayFlow needs a senior full-stack engineer to build the core of our B2B payment processing API. This includes designing secure transaction flows, implementing webhook systems, integrating third-party payment gateways (Stripe, Flouci), and building an admin dashboard for real-time monitoring. Security is paramount: PCI-DSS awareness, encryption at rest and in transit, fraud detection logic, and audit logging. Backend will be Django REST Framework with PostgreSQL. Frontend is React with TypeScript. Infrastructure runs on AWS (EC2, RDS, S3, SQS) with Redis for caching and rate limiting. The candidate must have prior fintech or payment systems experience, understand idempotency in payment flows, and be comfortable with high-availability architecture. Experience with financial reconciliation and double-entry bookkeeping logic is a strong advantage.`,
    keyRequirements: [
      "Django REST Framework API design",
      "Stripe or payment gateway integration",
      "PCI-DSS security awareness",
      "AWS infrastructure (EC2, RDS, SQS)",
      "Redis caching and rate limiting",
      "Fraud detection logic",
      "Idempotent payment flows",
      "Audit logging and compliance",
    ],
    dealbreakers: [
      "No payment systems experience",
      "No security/encryption knowledge",
      "No AWS experience",
    ],
    niceToHave: ["Financial reconciliation", "Double-entry bookkeeping", "SOC2 compliance"],
  },
  {
    id: 3,
    title: "Backend API Developer – Logistics & Tracking",
    company: "LogiStack",
    category: "Backend",
    budget: { min: 5000, max: 7000 },
    duration: 45,
    level: "Mid",
    skills: ["Node.js", "Express", "MongoDB", "Docker", "WebSockets", "Swagger"],
    posted: "1d ago",
    applicants: 22,
    description: `We need a backend developer to build RESTful APIs for our logistics and shipment tracking platform. Core features include real-time shipment status updates via WebSockets, driver location tracking, route optimization integration with Google Maps API, and a notification system (SMS via Twilio, email via SendGrid). Data is stored in MongoDB with Mongoose ODM. All APIs must be well-documented with Swagger/OpenAPI 3.0. Docker containerization is required for all services. Clean code principles, proper error handling, structured logging, and API versioning (v1/v2) are non-negotiable. The candidate should understand geospatial queries in MongoDB and have experience building event-driven architectures. Bonus: experience with Kafka or RabbitMQ for message queuing.`,
    keyRequirements: [
      "Node.js + Express REST API design",
      "MongoDB + geospatial queries",
      "WebSocket real-time updates",
      "Docker containerization",
      "Swagger/OpenAPI documentation",
      "Third-party integrations (Twilio, SendGrid)",
      "API versioning strategy",
      "Structured logging and error handling",
    ],
    dealbreakers: [
      "No MongoDB experience",
      "Never built real-time features",
      "No Docker knowledge",
    ],
    niceToHave: ["Kafka / RabbitMQ", "Kubernetes", "Google Maps API integration"],
  },
  {
    id: 4,
    title: "DevOps / Cloud Architect – Infrastructure Overhaul",
    company: "NebulaSys",
    category: "DevOps",
    budget: { min: 12000, max: 20000 },
    duration: 120,
    level: "Expert",
    skills: ["AWS", "Terraform", "Kubernetes", "CI/CD", "Linux", "Prometheus", "Grafana"],
    posted: "3d ago",
    applicants: 6,
    description: `NebulaSys is undergoing a full infrastructure overhaul from a monolithic VM-based setup to a cloud-native microservices architecture. We need an expert DevOps engineer to design and implement the entire infrastructure using Terraform IaC on AWS. This includes setting up EKS (Kubernetes), configuring auto-scaling groups, implementing a zero-downtime blue-green deployment pipeline with GitHub Actions, setting up centralized monitoring with Prometheus and Grafana, distributed tracing with Jaeger, and log aggregation with ELK stack. Security hardening is required: VPC design, IAM roles with least privilege, secrets management with AWS Secrets Manager, and WAF configuration. The candidate must have proven experience migrating monoliths to microservices and understand service mesh architectures (Istio or Linkerd).`,
    keyRequirements: [
      "Terraform IaC on AWS",
      "Kubernetes / EKS cluster management",
      "CI/CD with GitHub Actions (blue-green deploy)",
      "Prometheus + Grafana monitoring",
      "ELK stack log aggregation",
      "VPC design and IAM security",
      "Monolith to microservices migration",
      "Service mesh (Istio / Linkerd)",
    ],
    dealbreakers: [
      "No Kubernetes production experience",
      "No Terraform IaC experience",
      "No AWS certifications or deep AWS knowledge",
    ],
    niceToHave: ["AWS Solutions Architect certification", "Jaeger distributed tracing", "FinOps cost optimization"],
  },
  {
    id: 5,
    title: "Mobile Developer – React Native Cross-Platform App",
    company: "HealthSync",
    category: "Mobile",
    budget: { min: 7000, max: 10000 },
    duration: 75,
    level: "Mid",
    skills: ["React Native", "TypeScript", "Firebase", "Redux", "iOS", "Android"],
    posted: "6h ago",
    applicants: 11,
    description: `HealthSync is building a cross-platform health tracking mobile app for iOS and Android using React Native with TypeScript. The app includes real-time health metric syncing (heart rate, steps, sleep) via Apple HealthKit and Google Fit APIs, push notifications with Firebase Cloud Messaging, offline-first data storage with AsyncStorage and SQLite, user authentication with biometric login (FaceID/TouchID), and an interactive charting dashboard. The app must achieve 60fps smooth animations using Reanimated 2 and Gesture Handler. We need someone who has published apps to both App Store and Google Play, understands native module bridging, and can handle performance profiling and crash reporting with Sentry. HIPAA compliance awareness is a strong plus.`,
    keyRequirements: [
      "React Native + TypeScript production apps",
      "Published to App Store and Google Play",
      "Firebase (FCM, Firestore, Auth)",
      "HealthKit and Google Fit integration",
      "Reanimated 2 and Gesture Handler",
      "Offline-first architecture (SQLite)",
      "Biometric authentication",
      "Sentry crash reporting",
    ],
    dealbreakers: [
      "Never published a mobile app",
      "No TypeScript experience",
      "No Firebase experience",
    ],
    niceToHave: ["HIPAA compliance knowledge", "Native module bridging", "App Store Optimization"],
  },
];

// ─── SCORING ENGINE (NO API — PURE ALGORITHM) ─────────────────────────────────

const DIMENSION_WEIGHTS = {
  technicalRelevance:    { weight: 0.28, label: "Technical Relevance",    icon: "⚙️", color: "#6c63ff" },
  problemUnderstanding:  { weight: 0.22, label: "Problem Understanding",  icon: "🎯", color: "#48cfad" },
  experienceSignals:     { weight: 0.20, label: "Experience Signals",     icon: "📁", color: "#f9ca24" },
  communicationQuality:  { weight: 0.15, label: "Communication Quality",  icon: "🗣️", color: "#e17055" },
  valueProposition:      { weight: 0.10, label: "Value Proposition",      icon: "💎", color: "#a29bfe" },
  dealBreakerCheck:      { weight: 0.05, label: "Dealbreaker Check",      icon: "🚫", color: "#fd79a8" },
};

// ── keyword banks ──
const EXPERIENCE_KEYWORDS = [
  "years", "built", "developed", "shipped", "led", "designed", "implemented",
  "delivered", "worked on", "responsible for", "managed", "created", "launched",
  "production", "clients", "team", "project", "i have", "i've", "my experience",
  "previously", "currently", "past", "portfolio", "worked with",
];

const CONFIDENCE_POSITIVE = [
  "confident", "certain", "guarantee", "ensure", "proven", "expert", "specialist",
  "deep knowledge", "extensive", "proficient", "strong", "solid", "hands-on",
  "passionate", "dedicated", "committed", "deliver", "on time", "quality",
];

const CONFIDENCE_NEGATIVE = [
  "maybe", "possibly", "i think", "i guess", "not sure", "might", "could try",
  "learning", "beginner", "junior", "limited", "basic knowledge",
];

const VAGUE_PHRASES = [
  "i am a developer", "i can do this", "i am interested", "please hire me",
  "i will do my best", "i am available", "contact me", "i am good at",
  "i know everything", "fast learner", "hard worker", "team player",
  "i can handle", "no problem", "easy task",
];

const SPECIFICITY_MARKERS = [
  "specifically", "in particular", "for example", "such as", "including",
  "using", "with", "via", "through", "by implementing", "by building",
  "the reason", "because", "therefore", "which means", "this allows",
];

function tokenize(text) {
  return text.toLowerCase().replace(/[^a-z0-9\s]/g, " ").split(/\s+/).filter(Boolean);
}

function countMatches(text, keywords) {
  const lower = text.toLowerCase();
  return keywords.filter(kw => lower.includes(kw.toLowerCase())).length;
}

function getSentenceCount(text) {
  return text.split(/[.!?]+/).filter(s => s.trim().length > 10).length;
}

function getWordCount(text) {
  return text.trim().split(/\s+/).filter(Boolean).length;
}

function getAverageSentenceLength(text) {
  const sentences = text.split(/[.!?]+/).filter(s => s.trim().length > 5);
  if (!sentences.length) return 0;
  const total = sentences.reduce((sum, s) => sum + s.trim().split(/\s+/).length, 0);
  return total / sentences.length;
}

function scoreTechnicalRelevance(pitch, job) {
  const pitchLower = pitch.toLowerCase();
  const pitchTokens = tokenize(pitch);

  // Direct skill mentions
  const skillMatches = job.skills.filter(skill =>
    pitchLower.includes(skill.toLowerCase())
  );
  const skillScore = (skillMatches.length / job.skills.length) * 100;

  // Key requirement coverage
  const reqWords = job.keyRequirements.flatMap(r => tokenize(r));
  const pitchTokenSet = new Set(pitchTokens);
  const reqCoverage = reqWords.filter(w => w.length > 4 && pitchTokenSet.has(w)).length;
  const reqScore = Math.min((reqCoverage / Math.max(reqWords.filter(w => w.length > 4).length, 1)) * 150, 100);

  // Technical depth — are they using precise technical terms?
  const technicalTerms = [
    "api", "rest", "graphql", "sql", "nosql", "cache", "async", "sync",
    "deploy", "docker", "container", "server", "client", "database", "query",
    "index", "optimize", "refactor", "architect", "pipeline", "auth", "token",
    "webhook", "socket", "stream", "batch", "queue", "microservice", "monolith",
    "cdn", "load balanc", "scale", "latency", "throughput", "concurren",
  ];
  const techDepth = countMatches(pitch, technicalTerms);
  const techDepthScore = Math.min((techDepth / 8) * 100, 100);

  // Nice-to-have bonuses
  const niceMatches = job.niceToHave.filter(n => pitchLower.includes(n.toLowerCase())).length;
  const bonusScore = (niceMatches / Math.max(job.niceToHave.length, 1)) * 20;

  const final = Math.min(Math.round(skillScore * 0.45 + reqScore * 0.35 + techDepthScore * 0.15 + bonusScore * 0.05), 100);

  const missing = job.skills.filter(s => !pitchLower.includes(s.toLowerCase()));
  const matched = skillMatches;

  return {
    score: final,
    details: { skillScore: Math.round(skillScore), reqScore: Math.round(reqScore), techDepthScore: Math.round(techDepthScore) },
    matched,
    missing,
  };
}

function scoreProblemUnderstanding(pitch, job) {
  const pitchLower = pitch.toLowerCase();

  // Does pitch mention the client's business/domain?
  const domainWords = tokenize(job.description).filter(w => w.length > 5);
  const pitchTokens = new Set(tokenize(pitch));
  const domainOverlap = domainWords.filter(w => pitchTokens.has(w)).length;
  const domainScore = Math.min((domainOverlap / Math.max(domainWords.length * 0.15, 1)) * 100, 100);

  // Specificity — do they reference specific project details?
  const specificityScore = Math.min(countMatches(pitch, SPECIFICITY_MARKERS) * 12, 100);

  // Do they mention the actual problem to solve (not just skills)?
  const problemWords = ["challenge", "solution", "problem", "need", "require",
    "goal", "objective", "result", "outcome", "deliver", "achieve", "ensure",
    "improve", "optimize", "scalab", "perform", "secur", "reliab", "maintainab"];
  const problemScore = Math.min(countMatches(pitch, problemWords) * 10, 100);

  // Do they propose an approach?
  const approachWords = ["approach", "would", "plan", "strategy", "first", "then",
    "start by", "begin with", "structure", "architect", "design", "setup", "configure"];
  const approachScore = Math.min(countMatches(pitch, approachWords) * 12, 100);

  const final = Math.round(domainScore * 0.35 + specificityScore * 0.25 + problemScore * 0.20 + approachScore * 0.20);
  return { score: Math.min(final, 100) };
}

function scoreExperienceSignals(pitch, job) {
  const wordCount = getWordCount(pitch);

  // Concrete experience language
  const expCount = countMatches(pitch, EXPERIENCE_KEYWORDS);
  const expScore = Math.min((expCount / 6) * 100, 100);

  // Numbers and metrics (quantified achievements)
  const numbers = (pitch.match(/\d+(\+|k|%|x|\s*years|\s*months|\s*clients|\s*projects|\s*users|\s*ms)?/gi) || []).length;
  const metricsScore = Math.min(numbers * 12, 100);

  // Portfolio / proof signals
  const portfolioWords = ["github", "portfolio", "project", "demo", "link",
    "example", "case study", "built for", "worked with", "delivered to",
    "client", "production", "live", "deployed", "published"];
  const portfolioScore = Math.min(countMatches(pitch, portfolioWords) * 10, 100);

  // Length signal — too short = not serious
  const lengthScore = wordCount < 50 ? 20 : wordCount < 100 ? 50 : wordCount < 200 ? 75 : wordCount < 400 ? 95 : 85;

  // Level match signal
  const seniorWords = ["led", "architected", "mentored", "designed system", "owned",
    "responsible", "senior", "lead", "principal", "years of experience"];
  const juniorWords = ["learning", "studying", "beginner", "first job", "recent graduate",
    "just started", "entry level"];
  const seniorCount = countMatches(pitch, seniorWords);
  const juniorCount = countMatches(pitch, juniorWords);
  const levelMap = { Junior: 1, Mid: 2, Senior: 3, Expert: 4 };
  const jobLevel = levelMap[job.level] || 2;
  const levelPenalty = juniorCount > 0 && jobLevel >= 3 ? -20 : 0;
  const levelBonus = seniorCount > 0 && jobLevel >= 3 ? 10 : 0;

  const final = Math.round(expScore * 0.30 + metricsScore * 0.25 + portfolioScore * 0.25 + lengthScore * 0.20) + levelPenalty + levelBonus;
  return { score: Math.min(Math.max(final, 0), 100), wordCount, numbers };
}

function scoreCommunicationQuality(pitch) {
  const wordCount = getWordCount(pitch);
  const sentences = getSentenceCount(pitch);
  const avgLen = getAverageSentenceLength(pitch);

  // Confidence language
  const posCount = countMatches(pitch, CONFIDENCE_POSITIVE);
  const negCount = countMatches(pitch, CONFIDENCE_NEGATIVE);
  const confidenceScore = Math.min(Math.max((posCount * 12) - (negCount * 15), 0), 100);

  // Vague phrases penalty
  const vagueCount = countMatches(pitch, VAGUE_PHRASES);
  const vaguePenalty = vagueCount * 10;

  // Structure quality — good length sentences (not too short, not rambling)
  const structureScore = avgLen >= 10 && avgLen <= 25 ? 90 : avgLen < 10 ? 50 : 65;

  // Paragraph breaks (indicates organized writing)
  const paragraphs = pitch.split(/\n+/).filter(p => p.trim().length > 20).length;
  const paragraphScore = paragraphs >= 2 ? 85 : paragraphs === 1 ? 60 : 40;

  // Spelling/formality proxy — uppercase at sentence start, no all-caps words
  const allCapsWords = (pitch.match(/\b[A-Z]{3,}\b/g) || []).filter(w => !["API", "SQL", "AWS", "CSS", "HTML", "REST", "SDK", "MVP", "SaaS", "CI", "CD", "URL", "HTTP", "HTTPS", "JSON", "XML", "JWT", "ORM", "OOP", "DDD", "TDD", "BDD", "MVC", "SPA", "PWA", "SSR", "SSG", "CDN", "VPC", "IAM", "EKS", "RDS", "S3", "EC2", "SQS", "SNS", "FCM"].includes(w)).length;
  const formalityScore = Math.max(100 - allCapsWords * 10, 40);

  const raw = Math.round(confidenceScore * 0.30 + structureScore * 0.25 + paragraphScore * 0.25 + formalityScore * 0.20) - vaguePenalty;
  return { score: Math.min(Math.max(raw, 0), 100), vagueCount, posCount, negCount };
}

function scoreValueProposition(pitch, job) {
  const pitchLower = pitch.toLowerCase();

  // Do they say WHY them specifically?
  const whyMeWords = ["unique", "differ", "speciali", "focus", "passion",
    "best fit", "perfect fit", "ideal", "exactly what", "match", "align",
    "understand your", "your project", "your team", "your company",
    "what sets me", "my advantage", "i bring", "i offer"];
  const whyMeScore = Math.min(countMatches(pitch, whyMeWords) * 14, 100);

  // Timeline / delivery commitment
  const timelineWords = ["deadline", "on time", "deliver", "schedule", "timeline",
    "within", "days", "weeks", "milestone", "phase", "sprint"];
  const timelineScore = Math.min(countMatches(pitch, timelineWords) * 12, 100);

  // Communication commitment
  const commWords = ["update", "report", "communicate", "available", "response",
    "standup", "progress", "transparent", "daily", "weekly"];
  const commScore = Math.min(countMatches(pitch, commWords) * 14, 100);

  // Personalization — do they mention the company or job title?
  const companyMentioned = pitchLower.includes(job.company.toLowerCase());
  const titleWords = job.title.toLowerCase().split(" ").filter(w => w.length > 3);
  const titleMentioned = titleWords.some(w => pitchLower.includes(w));
  const personalScore = (companyMentioned ? 40 : 0) + (titleMentioned ? 30 : 0);

  const final = Math.round(whyMeScore * 0.35 + timelineScore * 0.25 + commScore * 0.20 + Math.min(personalScore, 100) * 0.20);
  return { score: Math.min(final, 100), companyMentioned, titleMentioned };
}

function checkDealbreakers(pitch, job) {
  const pitchLower = pitch.toLowerCase();
  const hits = job.dealbreakers.filter(db => {
    const dbWords = tokenize(db).filter(w => w.length > 4);
    return dbWords.some(w => pitchLower.includes(w));
  });

  // Positive: if they explicitly address dealbreakers, boost score
  const addressed = job.dealbreakers.filter(db => {
    const dbWords = tokenize(db).filter(w => w.length > 4);
    const pitchTokens = new Set(tokenize(pitch));
    const overlap = dbWords.filter(w => pitchTokens.has(w)).length;
    return overlap >= Math.ceil(dbWords.length * 0.5);
  });

  const score = addressed.length > 0
    ? Math.min(60 + addressed.length * 15, 100)
    : 50;

  return { score, addressed, hits };
}

function computeFullAnalysis(pitch, job) {
  const tech = scoreTechnicalRelevance(pitch, job);
  const prob = scoreProblemUnderstanding(pitch, job);
  const exp = scoreExperienceSignals(pitch, job);
  const comm = scoreCommunicationQuality(pitch);
  const val = scoreValueProposition(pitch, job);
  const db = checkDealbreakers(pitch, job);

  const overall = Math.round(
    tech.score  * DIMENSION_WEIGHTS.technicalRelevance.weight +
    prob.score  * DIMENSION_WEIGHTS.problemUnderstanding.weight +
    exp.score   * DIMENSION_WEIGHTS.experienceSignals.weight +
    comm.score  * DIMENSION_WEIGHTS.communicationQuality.weight +
    val.score   * DIMENSION_WEIGHTS.valueProposition.weight +
    db.score    * DIMENSION_WEIGHTS.dealBreakerCheck.weight
  );

  // Key matches
  const keyMatches = [];
  if (tech.matched.length) keyMatches.push(`Mentions ${tech.matched.length} required skills: ${tech.matched.slice(0, 4).join(", ")}`);
  if (exp.numbers > 2) keyMatches.push(`Uses ${exp.numbers} quantified data points (numbers/metrics) — signals credibility`);
  if (comm.posCount > 2) keyMatches.push("Uses confident, professional language throughout the pitch");
  if (val.companyMentioned) keyMatches.push(`Personalizes pitch by mentioning the company name (${job.company})`);
  if (db.addressed.length) keyMatches.push(`Addresses ${db.addressed.length} potential dealbreaker(s) proactively`);
  if (exp.wordCount >= 150) keyMatches.push(`Substantial pitch length (${exp.wordCount} words) shows effort and seriousness`);

  // Key gaps
  const keyGaps = [];
  if (tech.missing.length) keyGaps.push(`Does not mention: ${tech.missing.slice(0, 4).join(", ")} — core required skills`);
  if (exp.numbers < 2) keyGaps.push("No quantified achievements or metrics — vague experience claims");
  if (!val.companyMentioned && !val.titleMentioned) keyGaps.push("Pitch is not personalized to this specific job or company");
  if (comm.vagueCount > 1) keyGaps.push(`Contains ${comm.vagueCount} vague/generic phrase(s) that weaken the pitch`);
  if (exp.wordCount < 100) keyGaps.push("Pitch is too short — not enough detail to evaluate the candidate fairly");
  if (comm.negCount > 0) keyGaps.push("Uses uncertain language (maybe, might, could try) which reduces confidence");

  // Red flags
  const redFlags = [];
  if (comm.vagueCount > 3) redFlags.push("Excessive generic phrases — pitch reads as a template, not personalized");
  if (exp.wordCount < 50) redFlags.push("Critically short pitch — signals low effort or lack of experience");
  if (comm.negCount > 2) redFlags.push("Multiple instances of uncertain language signal low confidence");
  if (tech.score < 25) redFlags.push("Very low technical overlap — candidate may not have the required stack");
  if (overall < 30) redFlags.push("Overall very weak match — significant gaps in all key dimensions");

  // Improvement advice
  const improvements = [];
  if (tech.missing.length > 2) improvements.push(`Explicitly mention your experience with ${tech.missing.slice(0, 3).join(", ")} — even if limited.`);
  if (exp.numbers < 3) improvements.push("Add specific numbers: years of experience, number of projects, team sizes, performance improvements (e.g. 'reduced load time by 40%').");
  if (!val.companyMentioned) improvements.push(`Mention ${job.company} by name and reference a specific detail from their project description to show you read it carefully.`);
  if (comm.vagueCount > 0) improvements.push("Replace generic phrases like 'I can do this' or 'fast learner' with specific examples of what you've done.");
  if (exp.wordCount < 150) improvements.push("Expand your pitch to at least 150-250 words — detail your approach, past work, and why you specifically fit this project.");

  // Bid estimation
  const budgetMid = (job.budget.min + job.budget.max) / 2;
  const recommendedBid = Math.round(budgetMid * (0.85 + (overall / 100) * 0.25));
  const winProbability = Math.round(Math.max(5, Math.min(95, (overall / 100) * 85 + (1 - job.applicants / 30) * 15)));

  const hiringLikelihood = overall >= 80 ? "Very High" : overall >= 65 ? "High" : overall >= 45 ? "Medium" : "Low";
  const toneAnalysis = comm.negCount > 2 ? "Underconfident" : comm.posCount > 5 && comm.negCount === 0 ? "Overconfident" : comm.posCount > 2 ? "Professional" : comm.vagueCount > 2 ? "Casual" : "Balanced";

  return {
    overall,
    dimensions: [
      { key: "technicalRelevance",   score: tech.score,  details: tech  },
      { key: "problemUnderstanding", score: prob.score,  details: prob  },
      { key: "experienceSignals",    score: exp.score,   details: exp   },
      { key: "communicationQuality", score: comm.score,  details: comm  },
      { key: "valueProposition",     score: val.score,   details: val   },
      { key: "dealBreakerCheck",     score: db.score,    details: db    },
    ],
    keyMatches,
    keyGaps,
    redFlags,
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

// ─── HELPERS ──────────────────────────────────────────────────────────────────

const fmt = (n) => new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND", maximumFractionDigits: 0 }).format(n);
const scoreColor = (s) => s >= 80 ? "#6ab04c" : s >= 65 ? "#48cfad" : s >= 45 ? "#f9ca24" : s >= 30 ? "#e17055" : "#e74c3c";
const scoreLabel = (s) => s >= 80 ? "Excellent Match" : s >= 65 ? "Good Match" : s >= 45 ? "Partial Match" : s >= 30 ? "Weak Match" : "Poor Match";

// ─── SUB-COMPONENTS ───────────────────────────────────────────────────────────

function RadialScore({ score, size = 100 }) {
  const r = size / 2 - 9;
  const circ = 2 * Math.PI * r;
  const color = scoreColor(score);
  return (
    <div style={{ position: "relative", width: size, height: size, flexShrink: 0 }}>
      <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth={9} />
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={color} strokeWidth={9}
          strokeDasharray={`${(score/100)*circ} ${circ}`} strokeLinecap="round"
          style={{ transition: "stroke-dasharray 1s ease" }} />
      </svg>
      <div style={{ position:"absolute", inset:0, display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center" }}>
        <span style={{ fontSize: size > 90 ? 24 : 15, fontWeight: 900, color, lineHeight: 1 }}>{score}</span>
        <span style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontWeight: 600 }}>/100</span>
      </div>
    </div>
  );
}

function Bar({ value, color, delay = 0 }) {
  return (
    <div style={{ height: 7, background: "rgba(255,255,255,0.07)", borderRadius: 8, overflow: "hidden" }}>
      <div style={{ height:"100%", width:`${value}%`, background: color, borderRadius: 8, transition: `width 0.9s ease ${delay}s` }} />
    </div>
  );
}

// ─── MAIN COMPONENT ───────────────────────────────────────────────────────────

export default function PitchAnalyzer() {
  const [selectedJob, setSelectedJob] = useState(JOBS[0]);
  const [pitch, setPitch] = useState("");
  const [result, setResult] = useState(null);
  const [tab, setTab] = useState("input");

  const wordCount = pitch.trim().split(/\s+/).filter(Boolean).length;
  const charCount = pitch.length;

  const analyze = useCallback(() => {
    if (pitch.trim().length < 30) return;
    const r = computeFullAnalysis(pitch, selectedJob);
    setResult(r);
    setTab("result");
  }, [pitch, selectedJob]);

  const reset = () => { setResult(null); setPitch(""); setTab("input"); };

  const likeColor = { Low: "#e74c3c", Medium: "#f9ca24", High: "#48cfad", "Very High": "#6ab04c" };
  const toneColor = { Professional: "#6ab04c", Balanced: "#48cfad", Casual: "#f9ca24", Overconfident: "#e17055", Underconfident: "#e74c3c" };

  return (
    <div style={{ minHeight:"100vh", background:"linear-gradient(160deg,#08081a 0%,#0e0e2e 55%,#081a18 100%)", color:"#fff", fontFamily:"'Inter',system-ui,sans-serif" }}>
      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes fadeIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:translateY(0); } }
        * { box-sizing: border-box; }
        textarea:focus { border-color: rgba(108,99,255,0.5) !important; }
        ::-webkit-scrollbar { width: 5px; } ::-webkit-scrollbar-track { background: transparent; } ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; }
      `}</style>

      {/* NAV */}
      <nav style={{ borderBottom:"1px solid rgba(255,255,255,0.07)", padding:"0 32px", display:"flex", alignItems:"center", height:58, gap:16, background:"rgba(255,255,255,0.02)", backdropFilter:"blur(14px)", position:"sticky", top:0, zIndex:100 }}>
        <div style={{ width:34, height:34, borderRadius:9, background:"linear-gradient(135deg,#6c63ff,#48cfad)", display:"flex", alignItems:"center", justifyContent:"center", fontWeight:900, fontSize:14 }}>PA</div>
        <div>
          <span style={{ fontWeight:800, fontSize:16, letterSpacing:"-0.5px" }}>PitchAnalyzer </span>
          <span style={{ fontSize:10, background:"rgba(108,99,255,0.18)", color:"#a29bfe", padding:"2px 8px", borderRadius:20, fontWeight:700 }}>NO API · LOCAL ENGINE</span>
        </div>
        <div style={{ flex:1 }} />
        {result && (
          <button onClick={reset} style={{ padding:"7px 18px", background:"rgba(255,255,255,0.07)", border:"1px solid rgba(255,255,255,0.12)", borderRadius:8, color:"#fff", fontWeight:600, fontSize:13, cursor:"pointer" }}>
            ← New Analysis
          </button>
        )}
      </nav>

      <div style={{ maxWidth:1120, margin:"0 auto", padding:"28px 24px" }}>

        {/* ── INPUT TAB ── */}
        {tab === "input" && (
          <div style={{ display:"grid", gridTemplateColumns:"380px 1fr", gap:24, animation:"fadeIn 0.3s ease" }}>

            {/* Job List */}
            <div style={{ display:"flex", flexDirection:"column", gap:14 }}>
              <div style={{ fontSize:11, fontWeight:700, color:"rgba(255,255,255,0.35)", letterSpacing:1.2, textTransform:"uppercase", marginBottom:4 }}>Select a Project</div>
              {JOBS.map(j => (
                <div key={j.id} onClick={() => { setSelectedJob(j); setResult(null); }}
                  style={{ padding:"14px 16px", borderRadius:14, cursor:"pointer", border:`1px solid ${selectedJob.id===j.id ? "rgba(108,99,255,0.55)" : "rgba(255,255,255,0.07)"}`, background: selectedJob.id===j.id ? "rgba(108,99,255,0.1)" : "rgba(255,255,255,0.03)", transition:"all 0.2s" }}>
                  <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start" }}>
                    <div style={{ fontWeight:700, fontSize:13, lineHeight:1.4, flex:1, marginRight:8 }}>{j.title}</div>
                    <span style={{ fontSize:10, background:"rgba(255,255,255,0.07)", padding:"2px 8px", borderRadius:20, color:"rgba(255,255,255,0.45)", whiteSpace:"nowrap" }}>{j.level}</span>
                  </div>
                  <div style={{ fontSize:11, color:"rgba(255,255,255,0.4)", marginTop:4 }}>{j.company} · {fmt(j.budget.min)}–{fmt(j.budget.max)} · {j.duration}d · {j.applicants} applicants</div>
                  <div style={{ display:"flex", flexWrap:"wrap", gap:4, marginTop:8 }}>
                    {j.skills.slice(0,5).map(s => <span key={s} style={{ fontSize:10, padding:"2px 7px", borderRadius:20, background:"rgba(108,99,255,0.14)", color:"#a29bfe", fontWeight:600 }}>{s}</span>)}
                    {j.skills.length > 5 && <span style={{ fontSize:10, color:"rgba(255,255,255,0.3)" }}>+{j.skills.length-5}</span>}
                  </div>
                </div>
              ))}
            </div>

            {/* Right: Project Detail + Pitch Input */}
            <div style={{ display:"flex", flexDirection:"column", gap:18 }}>

              {/* Project breakdown */}
              <div style={{ background:"rgba(72,207,173,0.05)", borderRadius:16, padding:22, border:"1px solid rgba(72,207,173,0.13)" }}>
                <div style={{ fontSize:11, fontWeight:700, color:"#48cfad", letterSpacing:1.2, textTransform:"uppercase", marginBottom:14 }}>📋 Project Requirements — {selectedJob.title}</div>
                <p style={{ fontSize:13, color:"rgba(255,255,255,0.65)", lineHeight:1.8, margin:"0 0 14px" }}>{selectedJob.description}</p>
                <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:12 }}>
                  <div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", fontWeight:700, marginBottom:8, textTransform:"uppercase", letterSpacing:1 }}>Key Requirements</div>
                    {selectedJob.keyRequirements.map((r,i) => <div key={i} style={{ fontSize:12, color:"rgba(255,255,255,0.6)", padding:"4px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", display:"flex", gap:6 }}><span style={{ color:"#48cfad" }}>✓</span>{r}</div>)}
                  </div>
                  <div>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", fontWeight:700, marginBottom:8, textTransform:"uppercase", letterSpacing:1 }}>Dealbreakers</div>
                    {selectedJob.dealbreakers.map((d,i) => <div key={i} style={{ fontSize:12, color:"rgba(255,255,255,0.6)", padding:"4px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", display:"flex", gap:6 }}><span style={{ color:"#e74c3c" }}>✗</span>{d}</div>)}
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.35)", fontWeight:700, margin:"12px 0 8px", textTransform:"uppercase", letterSpacing:1 }}>Nice To Have</div>
                    {selectedJob.niceToHave.map((n,i) => <div key={i} style={{ fontSize:12, color:"rgba(255,255,255,0.6)", padding:"4px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", display:"flex", gap:6 }}><span style={{ color:"#f9ca24" }}>★</span>{n}</div>)}
                  </div>
                </div>
              </div>

              {/* Pitch Input */}
              <div style={{ background:"rgba(255,255,255,0.04)", borderRadius:16, padding:22, border:"1px solid rgba(255,255,255,0.08)" }}>
                <div style={{ fontSize:11, fontWeight:700, color:"rgba(255,255,255,0.35)", letterSpacing:1.2, textTransform:"uppercase", marginBottom:14 }}>✍️ Write Your Pitch</div>
                <textarea value={pitch} onChange={e => setPitch(e.target.value)}
                  placeholder={`Tell the client why you're the right fit. The more specific you are, the more accurate the analysis.\n\nTips for a high score:\n→ Mention specific technologies from the job (${selectedJob.skills.slice(0,3).join(", ")}...)\n→ Reference the actual project challenges\n→ Add numbers: years of experience, past project metrics\n→ Explain your approach to this specific problem\n→ Mention ${selectedJob.company} to show you read the listing`}
                  rows={12}
                  style={{ width:"100%", padding:"16px", background:"rgba(255,255,255,0.04)", border:"1px solid rgba(255,255,255,0.1)", borderRadius:12, color:"#fff", fontSize:14, resize:"none", outline:"none", lineHeight:1.8, fontFamily:"inherit", transition:"border-color 0.2s" }}
                />
                <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginTop:12 }}>
                  <div style={{ fontSize:12 }}>
                    <span style={{ color: wordCount >= 150 ? "#6ab04c" : wordCount >= 80 ? "#f9ca24" : "#e74c3c", fontWeight:700 }}>{wordCount} words</span>
                    <span style={{ color:"rgba(255,255,255,0.25)", marginLeft:8 }}>
                      {wordCount < 80 ? "— write more for accurate analysis" : wordCount < 150 ? "— good, add more detail" : "✓ great length"}
                    </span>
                  </div>
                  <button onClick={analyze} disabled={pitch.trim().length < 30}
                    style={{ padding:"11px 32px", background: pitch.trim().length < 30 ? "rgba(108,99,255,0.25)" : "linear-gradient(135deg,#6c63ff,#48cfad)", border:"none", borderRadius:10, color:"#fff", fontWeight:800, fontSize:14, cursor: pitch.trim().length < 30 ? "not-allowed" : "pointer", transition:"all 0.2s" }}>
                    🔍 Analyze Pitch
                  </button>
                </div>

                {/* Live preview bars while typing */}
                {pitch.length > 30 && (
                  <div style={{ marginTop:16, padding:"14px 16px", background:"rgba(255,255,255,0.03)", borderRadius:10, border:"1px solid rgba(255,255,255,0.06)" }}>
                    <div style={{ fontSize:11, color:"rgba(255,255,255,0.3)", marginBottom:10, fontWeight:700, textTransform:"uppercase", letterSpacing:1 }}>Live Signal Preview</div>
                    {(() => {
                      const preview = computeFullAnalysis(pitch, selectedJob);
                      return preview.dimensions.map((d, i) => {
                        const meta = DIMENSION_WEIGHTS[d.key];
                        return (
                          <div key={d.key} style={{ marginBottom:8 }}>
                            <div style={{ display:"flex", justifyContent:"space-between", fontSize:11, marginBottom:3 }}>
                              <span style={{ color:"rgba(255,255,255,0.4)" }}>{meta.icon} {meta.label}</span>
                              <span style={{ fontWeight:700, color: scoreColor(d.score) }}>{d.score}</span>
                            </div>
                            <Bar value={d.score} color={scoreColor(d.score)} delay={i * 0.05} />
                          </div>
                        );
                      });
                    })()}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── RESULT TAB ── */}
        {tab === "result" && result && (
          <div style={{ animation:"fadeIn 0.4s ease" }}>

            {/* Hero Score */}
            <div style={{ background:`linear-gradient(135deg, ${scoreColor(result.overall)}15, rgba(108,99,255,0.12))`, borderRadius:20, padding:"28px 32px", border:`1px solid ${scoreColor(result.overall)}30`, display:"flex", alignItems:"center", gap:28, marginBottom:24 }}>
              <RadialScore score={result.overall} size={120} />
              <div style={{ flex:1 }}>
                <div style={{ fontSize:28, fontWeight:900, color: scoreColor(result.overall), letterSpacing:"-1px", marginBottom:6 }}>{scoreLabel(result.overall)}</div>
                <div style={{ fontSize:14, color:"rgba(255,255,255,0.6)", marginBottom:16, lineHeight:1.6 }}>
                  Pitch analyzed against <strong style={{ color:"#fff" }}>{selectedJob.title}</strong> at <strong style={{ color:"#fff" }}>{selectedJob.company}</strong> · {result.wordCount} words analyzed
                </div>
                <div style={{ display:"flex", gap:10, flexWrap:"wrap" }}>
                  {[
                    [`🎯 Hiring: ${result.hiringLikelihood}`, likeColor[result.hiringLikelihood]],
                    [`🎤 Tone: ${result.toneAnalysis}`, toneColor[result.toneAnalysis]],
                    [`📊 Win Chance: ${result.winProbability}%`, scoreColor(result.winProbability)],
                    [`💰 Suggested Bid: ${fmt(result.recommendedBid)}`, "#a29bfe"],
                  ].map(([label, color]) => (
                    <span key={label} style={{ fontSize:12, padding:"5px 13px", borderRadius:20, background:`${color}20`, color, fontWeight:700, border:`1px solid ${color}40` }}>{label}</span>
                  ))}
                </div>
              </div>
              {/* Mini score grid */}
              <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:8, flexShrink:0 }}>
                {result.dimensions.map(d => {
                  const meta = DIMENSION_WEIGHTS[d.key];
                  return (
                    <div key={d.key} style={{ background:"rgba(255,255,255,0.04)", borderRadius:10, padding:"10px 12px", textAlign:"center", border:`1px solid ${scoreColor(d.score)}25`, minWidth:90 }}>
                      <div style={{ fontSize:16 }}>{meta.icon}</div>
                      <div style={{ fontSize:18, fontWeight:900, color: scoreColor(d.score) }}>{d.score}</div>
                      <div style={{ fontSize:10, color:"rgba(255,255,255,0.35)", lineHeight:1.3 }}>{meta.label.split(" ")[0]}</div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:20 }}>

              {/* Dimension Breakdown */}
              <div style={{ background:"rgba(255,255,255,0.04)", borderRadius:18, padding:24, border:"1px solid rgba(255,255,255,0.08)" }}>
                <div style={{ fontSize:11, fontWeight:700, color:"rgba(255,255,255,0.35)", letterSpacing:1.2, textTransform:"uppercase", marginBottom:18 }}>📊 Full Dimension Breakdown</div>
                {result.dimensions.map((d, i) => {
                  const meta = DIMENSION_WEIGHTS[d.key];
                  return (
                    <div key={d.key} style={{ marginBottom:20 }}>
                      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:7 }}>
                        <div style={{ display:"flex", alignItems:"center", gap:8 }}>
                          <span style={{ fontSize:15 }}>{meta.icon}</span>
                          <span style={{ fontWeight:700, fontSize:14 }}>{meta.label}</span>
                          <span style={{ fontSize:10, color:"rgba(255,255,255,0.3)", background:"rgba(255,255,255,0.05)", padding:"1px 7px", borderRadius:20 }}>×{(meta.weight*100).toFixed(0)}%</span>
                        </div>
                        <span style={{ fontWeight:900, fontSize:16, color: scoreColor(d.score) }}>{d.score}</span>
                      </div>
                      <Bar value={d.score} color={scoreColor(d.score)} delay={i * 0.08} />
                      <div style={{ height:6, background:"transparent", borderRadius:8, marginTop:2, position:"relative" }}>
                        <div style={{ position:"absolute", left:`${meta.weight*100*4}%`, top:-2, width:1, height:10, background:"rgba(255,255,255,0.15)" }} />
                      </div>
                    </div>
                  );
                })}

                {/* Skills breakdown */}
                <div style={{ marginTop:8, paddingTop:16, borderTop:"1px solid rgba(255,255,255,0.07)" }}>
                  <div style={{ fontSize:11, fontWeight:700, color:"rgba(255,255,255,0.35)", letterSpacing:1, textTransform:"uppercase", marginBottom:10 }}>Skills Coverage</div>
                  <div style={{ display:"flex", flexWrap:"wrap", gap:6 }}>
                    {selectedJob.skills.map(s => {
                      const matched = result.skillsMatched.includes(s);
                      return (
                        <span key={s} style={{ fontSize:11, padding:"4px 10px", borderRadius:20, fontWeight:700, background: matched ? "rgba(72,207,173,0.12)" : "rgba(231,76,60,0.1)", color: matched ? "#48cfad" : "#e74c3c", border:`1px solid ${matched ? "rgba(72,207,173,0.3)" : "rgba(231,76,60,0.2)"}` }}>
                          {matched ? "✓" : "✗"} {s}
                        </span>
                      );
                    })}
                  </div>
                  <div style={{ marginTop:10, fontSize:12, color:"rgba(255,255,255,0.4)" }}>
                    {result.skillsMatched.length}/{selectedJob.skills.length} required skills mentioned in pitch
                  </div>
                </div>
              </div>

              {/* Right column */}
              <div style={{ display:"flex", flexDirection:"column", gap:16 }}>

                {/* Matches */}
                <div style={{ background:"rgba(106,176,76,0.06)", borderRadius:16, padding:20, border:"1px solid rgba(106,176,76,0.15)" }}>
                  <div style={{ fontSize:11, fontWeight:700, color:"#6ab04c", letterSpacing:1.2, textTransform:"uppercase", marginBottom:12 }}>✅ What Works In Your Pitch</div>
                  {result.keyMatches.length > 0
                    ? result.keyMatches.map((m, i) => <div key={i} style={{ fontSize:13, color:"rgba(255,255,255,0.7)", padding:"7px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", lineHeight:1.6 }}>• {m}</div>)
                    : <div style={{ fontSize:13, color:"rgba(255,255,255,0.3)" }}>No strong signals detected yet.</div>
                  }
                </div>

                {/* Gaps */}
                <div style={{ background:"rgba(231,76,60,0.05)", borderRadius:16, padding:20, border:"1px solid rgba(231,76,60,0.14)" }}>
                  <div style={{ fontSize:11, fontWeight:700, color:"#e74c3c", letterSpacing:1.2, textTransform:"uppercase", marginBottom:12 }}>⚠️ What's Missing</div>
                  {result.keyGaps.length > 0
                    ? result.keyGaps.map((g, i) => <div key={i} style={{ fontSize:13, color:"rgba(255,255,255,0.7)", padding:"7px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", lineHeight:1.6 }}>• {g}</div>)
                    : <div style={{ fontSize:13, color:"#6ab04c" }}>No significant gaps — strong pitch!</div>
                  }
                </div>

                {/* Red Flags */}
                {result.redFlags.length > 0 && (
                  <div style={{ background:"rgba(225,112,85,0.07)", borderRadius:16, padding:20, border:"1px solid rgba(225,112,85,0.2)" }}>
                    <div style={{ fontSize:11, fontWeight:700, color:"#e17055", letterSpacing:1.2, textTransform:"uppercase", marginBottom:12 }}>🚩 Red Flags</div>
                    {result.redFlags.map((f, i) => <div key={i} style={{ fontSize:13, color:"rgba(255,255,255,0.7)", padding:"6px 0", lineHeight:1.6 }}>• {f}</div>)}
                  </div>
                )}

                {/* Improvement */}
                <div style={{ background:"linear-gradient(135deg,rgba(108,99,255,0.1),rgba(72,207,173,0.07))", borderRadius:16, padding:20, border:"1px solid rgba(108,99,255,0.2)" }}>
                  <div style={{ fontSize:11, fontWeight:700, color:"#a29bfe", letterSpacing:1.2, textTransform:"uppercase", marginBottom:12 }}>💡 How To Improve This Pitch</div>
                  {result.improvements.map((imp, i) => (
                    <div key={i} style={{ display:"flex", gap:10, padding:"7px 0", borderBottom:"1px solid rgba(255,255,255,0.05)", fontSize:13, color:"rgba(255,255,255,0.72)", lineHeight:1.65 }}>
                      <span style={{ color:"#6c63ff", fontWeight:700, flexShrink:0 }}>{i+1}.</span>
                      {imp}
                    </div>
                  ))}
                </div>

                {/* Bid recommendation */}
                <div style={{ background:"rgba(162,155,254,0.07)", borderRadius:16, padding:20, border:"1px solid rgba(162,155,254,0.18)" }}>
                  <div style={{ fontSize:11, fontWeight:700, color:"#a29bfe", letterSpacing:1.2, textTransform:"uppercase", marginBottom:14 }}>💰 Bid Intelligence</div>
                  {[
                    ["Client Budget Range", `${fmt(selectedJob.budget.min)} – ${fmt(selectedJob.budget.max)}`,"#fff"],
                    ["Recommended Bid", fmt(result.recommendedBid), "#a29bfe"],
                    ["Platform Fee (10%)", `-${fmt(result.recommendedBid * PLATFORM_COMMISSION)}`, "#e17055"],
                    ["TVA on Fee (19%)", `-${fmt(result.recommendedBid * PLATFORM_COMMISSION * TVA_RATE)}`, "#e17055"],
                    ["You Receive (net)", fmt(result.recommendedBid * (1 - PLATFORM_COMMISSION)), "#6ab04c"],
                    ["Win Probability", `${result.winProbability}%`, scoreColor(result.winProbability)],
                  ].map(([l,v,c]) => (
                    <div key={l} style={{ display:"flex", justifyContent:"space-between", padding:"8px 0", borderBottom:"1px solid rgba(255,255,255,0.06)", fontSize:13 }}>
                      <span style={{ color:"rgba(255,255,255,0.5)" }}>{l}</span>
                      <span style={{ fontWeight:700, color:c }}>{v}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div style={{ marginTop:20, textAlign:"center" }}>
              <button onClick={reset} style={{ padding:"12px 36px", background:"rgba(255,255,255,0.06)", border:"1px solid rgba(255,255,255,0.12)", borderRadius:12, color:"#fff", fontWeight:700, fontSize:14, cursor:"pointer" }}>
                ← Analyze Another Pitch
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
