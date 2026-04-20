import { useState, useCallback } from "react";

// —— Local rule-based analyzer (no external AI API) ——
function tokenize(text) {
  return (text || "")
    .toLowerCase()
    .replace(/[^\w\s]/g, " ")
    .split(/\s+/)
    .filter(Boolean);
}

function extractKeywords(text, minLen = 3) {
  const words = tokenize(text);
  const stop = new Set([
    "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "has", "his", "how", "its", "may", "new", "now", "old", "see", "way", "who", "did", "get", "got", "let", "put", "say", "she", "too", "use",
  ]);
  return [...new Set(words.filter((w) => w.length >= minLen && !stop.has(w)))];
}

function analyzePitchLocally(job, pitch) {
  const pitchLower = (pitch || "").toLowerCase();
  const pitchWords = tokenize(pitch);
  const pitchWordCount = pitchWords.length;
  const jobDesc = (job.description || "").toLowerCase();
  const jobSkills = (job.skills || []).map((s) => s.toLowerCase());
  const jobKeywords = [
    ...jobSkills,
    ...extractKeywords(job.description).slice(0, 25),
  ];
  const uniqJob = [...new Set(jobKeywords)];

  // Technical Relevance: skill + keyword overlap
  const mentionedSkills = jobSkills.filter((s) => pitchLower.includes(s));
  const mentionedKeywords = uniqJob.filter((kw) => pitchLower.includes(kw));
  const techScore = Math.min(
    100,
    Math.round(
      (mentionedSkills.length / Math.max(1, jobSkills.length)) * 50 +
        (mentionedKeywords.length / Math.max(1, uniqJob.length)) * 50
    )
  );
  const techExplanation =
    mentionedSkills.length >= jobSkills.length / 2
      ? `Your pitch mentions ${mentionedSkills.join(", ")} and aligns well with the required tech stack.`
      : mentionedSkills.length > 0
        ? `You mention ${mentionedSkills.join(", ")} but could strengthen alignment with ${jobSkills.filter((s) => !mentionedSkills.includes(s)).slice(0, 3).join(", ")}.`
        : `The pitch does not clearly reference the required skills (${jobSkills.join(", ")}). Add concrete technologies you would use.`;

  // Problem Understanding: project-specific terms in pitch
  const descTerms = extractKeywords(job.description, 4).slice(0, 20);
  const matchedTerms = descTerms.filter((t) => pitchLower.includes(t));
  const problemScore = Math.min(
    100,
    Math.round((matchedTerms.length / Math.max(1, descTerms.length)) * 100)
  );
  const problemExplanation =
    matchedTerms.length >= 5
      ? `You address several project-specific concepts, showing you read the brief.`
      : matchedTerms.length >= 2
        ? `Some project terms appear in your pitch; add more references to the client's exact problem.`
        : `Try to echo the client's language and problem (e.g. from the project description) so they see you understand the scope.`;

  // Experience Signals
  const expMarkers = [
    "experience", "years", "built", "shipped", "worked", "developed", "implemented",
    "led", "senior", "expert", "production", "previous", "portfolio", "client",
  ];
  const expCount = expMarkers.filter((m) => pitchLower.includes(m)).length;
  const hasNumbers = /\d+(\s*(\+)?\s*(years?|yr|projects?|clients?))?/i.test(pitch);
  const expScore = Math.min(
    100,
    Math.round(expCount * 12 + (hasNumbers ? 15 : 0) + (pitchWordCount >= 80 ? 15 : pitchWordCount >= 40 ? 8 : 0))
  );
  const expExplanation =
    expScore >= 70
      ? `You provide concrete experience signals and context.`
      : expScore >= 40
        ? `Add more specific experience (years, projects, outcomes) to build trust.`
        : `Include clear experience evidence: years, similar projects, or results.`;

  // Communication Quality
  const hasParagraphs = (pitch.match(/\n\n/g) || []).length >= 1;
  const exclamations = (pitch.match(/!/g) || []).length;
  const allCapsRatio =
    pitch.replace(/\s/g, "").length > 0
      ? (pitch.replace(/\s/g, "").match(/[A-Z]/g) || []).length /
        pitch.replace(/\s/g, "").length
      : 0;
  const commScore = Math.max(
    0,
    Math.min(
      100,
      30 +
        (pitchWordCount >= 80 ? 25 : pitchWordCount >= 50 ? 15 : 5) +
        (hasParagraphs ? 15 : 0) +
        (exclamations <= 2 ? 15 : exclamations <= 5 ? 5 : -10) +
        (allCapsRatio < 0.2 ? 15 : -15)
    )
  );
  const commExplanation =
    pitchWordCount >= 80 && exclamations <= 2
      ? `Clear structure and professional tone.`
      : pitchWordCount < 50
        ? `A longer, well-structured pitch (2–3 short paragraphs) usually performs better.`
        : exclamations > 4
          ? `Tone down exclamation marks for a more professional tone.`
          : `Consider breaking the pitch into short paragraphs for readability.`;

  // Value Proposition
  const valueMarkers = [
    "fit", "right choice", "best", "why i", "perfect", "ideal", "trust",
    "reliable", "deliver", "commit", "dedicated", "match", "align",
  ];
  const valueCount = valueMarkers.filter((m) => pitchLower.includes(m)).length;
  const valueScore = Math.min(100, 40 + valueCount * 15 + (pitchWordCount >= 60 ? 15 : 0));
  const valueExplanation =
    valueScore >= 60
      ? `You articulate why you are a good fit for this project.`
      : `Add a sentence on why you are the right choice for this specific client and project.`;

  // Overall
  const weights = { tech: 30, problem: 25, exp: 20, comm: 15, value: 10 };
  const overallScore = Math.round(
    (techScore * weights.tech +
      problemScore * weights.problem +
      expScore * weights.exp +
      commScore * weights.comm +
      valueScore * weights.value) /
      100
  );
  const clampedOverall = Math.max(0, Math.min(100, overallScore));

  const verdict =
    clampedOverall >= 75
      ? "Strong alignment with the project; your pitch addresses skills and context well."
      : clampedOverall >= 55
        ? "Good base; a few targeted additions could improve your match."
        : clampedOverall >= 35
          ? "Some overlap exists but the pitch could be much more specific to this project."
          : "The pitch is too generic or short; tailor it to the project and add evidence.";

  const keyMatches = mentionedSkills.length > 0
    ? mentionedSkills.map((s) => `Mentions required skill: ${s}`)
    : mentionedKeywords.slice(0, 5).map((k) => `Addresses: ${k}`);
  if (keyMatches.length === 0) keyMatches.push("Consider highlighting at least one required skill or keyword.");

  const keyGaps = jobSkills
    .filter((s) => !pitchLower.includes(s))
    .slice(0, 5)
    .map((s) => `Required skill not mentioned: ${s}`);
  if (keyGaps.length === 0) keyGaps.push("No major skill gaps detected.");

  const redFlags = [];
  if (pitchWordCount < 40) redFlags.push("Pitch is very short; clients often prefer more detail.");
  if (mentionedSkills.length === 0 && jobSkills.length > 0)
    redFlags.push("None of the required skills are explicitly mentioned.");
  if (exclamations > 5) redFlags.push("Too many exclamation marks can seem unprofessional.");
  if (allCapsRatio > 0.3) redFlags.push("Avoid writing in all caps.");

  const improvements = [];
  if (techScore < 50) improvements.push("Name the technologies you would use and how they match the stack.");
  if (problemScore < 50) improvements.push("Reference the client's problem or goals in their own words.");
  if (expScore < 50) improvements.push("Add concrete experience: years, similar projects, or outcomes.");
  if (commScore < 50) improvements.push("Use 2–3 short paragraphs and a professional tone.");
  if (valueScore < 50) improvements.push("Add one clear sentence on why you are the right fit.");
  const improvement =
    improvements.length > 0
      ? improvements.join(" ")
      : "Your pitch is well aligned; you could add one specific deliverable or timeline to stand out.";

  const hiringLikelihood =
    clampedOverall >= 80 ? "Very High" : clampedOverall >= 60 ? "High" : clampedOverall >= 40 ? "Medium" : "Low";

  let toneAnalysis = "Balanced";
  if (exclamations > 5 && pitchWordCount < 100) toneAnalysis = "Overconfident";
  else if (pitchWordCount < 40 && expCount < 2) toneAnalysis = "Underconfident";
  else if (hasParagraphs && expCount >= 3 && exclamations <= 1) toneAnalysis = "Professional";
  else if (pitchWordCount < 60 && exclamations >= 2) toneAnalysis = "Casual";

  return {
    overallScore: clampedOverall,
    verdict,
    dimensions: [
      { name: "Technical Relevance", score: techScore, explanation: techExplanation, weight: 30 },
      { name: "Problem Understanding", score: problemScore, explanation: problemExplanation, weight: 25 },
      { name: "Experience Signals", score: expScore, explanation: expExplanation, weight: 20 },
      { name: "Communication Quality", score: commScore, explanation: commExplanation, weight: 15 },
      { name: "Value Proposition", score: valueScore, explanation: valueExplanation, weight: 10 },
    ],
    keyMatches,
    keyGaps,
    redFlags: redFlags.length ? redFlags : undefined,
    improvement,
    hiringLikelihood,
    toneAnalysis,
  };
}

const JOBS = [
  {
    id: 1,
    title: "Senior React Developer – SaaS Dashboard",
    company: "TechVision",
    budget: { min: 8000, max: 12000 },
    duration: 60,
    skills: ["React", "TypeScript", "Node.js", "PostgreSQL"],
    level: "Senior",
    description: `We are building a complex SaaS analytics dashboard for B2B clients. 
The platform must handle real-time data streams, complex chart visualizations using D3 or Recharts, 
multi-tenant architecture, role-based access control, and must be highly performant with large datasets.
We need someone who deeply understands React state management (Redux/Zustand), can build reusable 
component libraries, and has shipped production-grade TypeScript codebases before. 
API integration with REST and GraphQL endpoints is required. The candidate must also have experience 
with CI/CD pipelines and writing unit/integration tests.`,
  },
  {
    id: 2,
    title: "Full-Stack Engineer – FinTech Payment Platform",
    company: "PayFlow",
    budget: { min: 10000, max: 15000 },
    duration: 90,
    skills: ["Python", "Django", "React", "AWS", "Redis"],
    level: "Senior",
    description: `PayFlow needs a senior full-stack engineer to build the core of our B2B payment processing API. 
This includes designing secure transaction flows, implementing webhook systems, integrating third-party 
payment gateways (Stripe, Flouci), and building an admin dashboard for monitoring. 
Security is paramount: PCI-DSS awareness, encryption at rest and in transit, fraud detection logic. 
Backend will be Django REST Framework, frontend React. Infrastructure on AWS with Redis for caching. 
Candidate must have prior fintech or payment systems experience.`,
  },
  {
    id: 3,
    title: "Backend API Developer – Logistics Platform",
    company: "LogiStack",
    budget: { min: 5000, max: 7000 },
    duration: 45,
    skills: ["Node.js", "Express", "MongoDB", "Docker"],
    level: "Mid",
    description: `We need a backend developer to build RESTful APIs for our logistics and shipment tracking platform. 
Core features include real-time shipment status updates, driver location tracking via WebSockets, 
route optimization integration, and a notification system (SMS/email). 
Data is stored in MongoDB. APIs must be well-documented with Swagger. Docker containerization required. 
Clean code, proper error handling, and API versioning are non-negotiable. 
Bonus: experience with mapping APIs (Google Maps, Leaflet).`,
  },
];

const fmt = (n) =>
  new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND", maximumFractionDigits: 0 }).format(n);

const SCORE_COLOR = (s) =>
  s >= 80 ? "#6ab04c" : s >= 60 ? "#48cfad" : s >= 40 ? "#f9ca24" : "#e74c3c";

const SCORE_LABEL = (s) =>
  s >= 80 ? "Excellent Match" : s >= 60 ? "Good Match" : s >= 40 ? "Partial Match" : "Weak Match";

function AnimatedBar({ value, color, delay = 0 }) {
  return (
    <div style={{ height: 8, background: "rgba(255,255,255,0.07)", borderRadius: 8, overflow: "hidden" }}>
      <div
        style={{
          height: "100%",
          width: `${value}%`,
          background: color,
          borderRadius: 8,
          transition: `width 1s ease ${delay}s`,
        }}
      />
    </div>
  );
}

function RadialScore({ score, size = 100 }) {
  const r = size / 2 - 8;
  const circ = 2 * Math.PI * r;
  const color = SCORE_COLOR(score);
  return (
    <div style={{ position: "relative", width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="rgba(255,255,255,0.07)" strokeWidth={8} />
        <circle
          cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={8}
          strokeDasharray={`${(score / 100) * circ} ${circ}`} strokeLinecap="round"
          style={{ transition: "stroke-dasharray 1.2s ease" }}
        />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
        <span style={{ fontSize: size > 80 ? 26 : 16, fontWeight: 900, color }}>{score}</span>
        <span style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", fontWeight: 600 }}>/100</span>
      </div>
    </div>
  );
}

export default function PitchAnalyzer() {
  const [selectedJob, setSelectedJob] = useState(JOBS[0]);
  const [pitch, setPitch] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [charCount, setCharCount] = useState(0);

  const handlePitchChange = (e) => {
    setPitch(e.target.value);
    setCharCount(e.target.value.length);
  };

  const analyzeMatch = useCallback(() => {
    if (pitch.trim().length < 50) {
      setError("Please write at least 50 characters for a meaningful analysis.");
      return;
    }
    setError(null);
    setLoading(true);
    setResult(null);

    // Simulate short delay, then run local rule-based analyzer (no API)
    setTimeout(() => {
      try {
        const parsed = analyzePitchLocally(selectedJob, pitch);
        setResult(parsed);
      } catch (err) {
        setError("Analysis failed. Please try again.");
        console.error(err);
      } finally {
        setLoading(false);
      }
    }, 800);
  }, [pitch, selectedJob]);

  const likeColor = {
    Low: "#e74c3c",
    Medium: "#f9ca24",
    High: "#48cfad",
    "Very High": "#6ab04c",
  };

  const toneColor = {
    Professional: "#6ab04c",
    Balanced: "#48cfad",
    Casual: "#f9ca24",
    Overconfident: "#e17055",
    Underconfident: "#e74c3c",
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(160deg, #0a0a1a 0%, #0f0f2e 50%, #0a1a1a 100%)",
        color: "#fff",
        fontFamily: "'Inter', system-ui, sans-serif",
      }}
    >
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>

      {/* Header */}
      <div
        style={{
          borderBottom: "1px solid rgba(255,255,255,0.07)",
          padding: "16px 32px",
          display: "flex",
          alignItems: "center",
          gap: 14,
          background: "rgba(255,255,255,0.02)",
          backdropFilter: "blur(12px)",
        }}
      >
        <div
          style={{
            width: 36, height: 36, borderRadius: 10,
            background: "linear-gradient(135deg, #6c63ff, #48cfad)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontWeight: 900, fontSize: 16,
          }}
        >
          AI
        </div>
        <div>
          <div style={{ fontWeight: 800, fontSize: 17, letterSpacing: "-0.5px" }}>
            PitchAnalyzer{" "}
            <span
              style={{
                color: "#6c63ff", fontSize: 11,
                background: "rgba(108,99,255,0.15)",
                padding: "2px 8px", borderRadius: 20,
              }}
            >
              LOCAL · NO API
            </span>
          </div>
          <div style={{ fontSize: 12, color: "rgba(255,255,255,0.4)" }}>
            Semantic match engine — freelancer pitch vs. project requirements
          </div>
        </div>
      </div>

      {/* Main Grid */}
      <div
        style={{
          maxWidth: 1100,
          margin: "0 auto",
          padding: "32px 24px",
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 28,
        }}
      >
        {/* LEFT PANEL */}
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>

          {/* Job Selector */}
          <div
            style={{
              background: "rgba(255,255,255,0.04)", borderRadius: 18,
              padding: 24, border: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <div
              style={{
                fontSize: 12, fontWeight: 700, color: "rgba(255,255,255,0.4)",
                letterSpacing: 1, marginBottom: 14, textTransform: "uppercase",
              }}
            >
              Select Project
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {JOBS.map((j) => (
                <div
                  key={j.id}
                  onClick={() => { setSelectedJob(j); setResult(null); }}
                  style={{
                    padding: "14px 16px", borderRadius: 12, cursor: "pointer",
                    border: `1px solid ${selectedJob.id === j.id ? "rgba(108,99,255,0.5)" : "rgba(255,255,255,0.06)"}`,
                    background: selectedJob.id === j.id ? "rgba(108,99,255,0.1)" : "rgba(255,255,255,0.02)",
                    transition: "all 0.2s",
                  }}
                >
                  <div style={{ fontWeight: 700, fontSize: 14 }}>{j.title}</div>
                  <div style={{ fontSize: 12, color: "rgba(255,255,255,0.45)", marginTop: 4 }}>
                    {j.company} · {fmt(j.budget.min)}–{fmt(j.budget.max)} · {j.duration}d
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 5, marginTop: 8 }}>
                    {j.skills.map((s) => (
                      <span
                        key={s}
                        style={{
                          fontSize: 10, padding: "2px 8px", borderRadius: 20,
                          background: "rgba(108,99,255,0.15)", color: "#a29bfe", fontWeight: 600,
                        }}
                      >
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Project Description */}
          <div
            style={{
              background: "rgba(72,207,173,0.05)", borderRadius: 18,
              padding: 22, border: "1px solid rgba(72,207,173,0.12)",
            }}
          >
            <div
              style={{
                fontSize: 12, fontWeight: 700, color: "#48cfad",
                letterSpacing: 1, marginBottom: 10, textTransform: "uppercase",
              }}
            >
              📋 What the Client Needs
            </div>
            <p
              style={{
                fontSize: 13, color: "rgba(255,255,255,0.65)",
                lineHeight: 1.75, margin: 0, maxHeight: 160, overflowY: "auto",
              }}
            >
              {selectedJob.description}
            </p>
          </div>

          {/* Pitch Input */}
          <div
            style={{
              background: "rgba(255,255,255,0.04)", borderRadius: 18,
              padding: 24, border: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <div
              style={{
                fontSize: 12, fontWeight: 700, color: "rgba(255,255,255,0.4)",
                letterSpacing: 1, marginBottom: 14, textTransform: "uppercase",
              }}
            >
              ✍️ Your Pitch to the Client
            </div>
            <textarea
              value={pitch}
              onChange={handlePitchChange}
              placeholder={`Write what you would tell the client. Be specific — mention your relevant experience, how you'd approach this exact project, tools you'd use, and why you're the right fit.\n\nThe AI will analyze how closely your pitch matches what the client actually needs...`}
              rows={10}
              style={{
                width: "100%", padding: "16px",
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.1)",
                borderRadius: 12, color: "#fff", fontSize: 14,
                boxSizing: "border-box", resize: "none", outline: "none",
                lineHeight: 1.8, fontFamily: "inherit",
              }}
            />
            <div style={{ marginTop: 12, fontSize: 12, color: charCount >= 150 ? "#6ab04c" : charCount >= 50 ? "#f9ca24" : "rgba(255,255,255,0.3)" }}>
              {charCount} chars{" "}
              {charCount >= 150 ? "✓ Great length" : charCount >= 50 ? "— add more detail" : "— too short"}
            </div>
            <button
              type="button"
              onClick={analyzeMatch}
              disabled={loading || pitch.trim().length < 10}
              style={{
                marginTop: 16,
                width: "100%",
                padding: "14px 24px",
                background: loading ? "rgba(108,99,255,0.3)" : "linear-gradient(135deg, #6c63ff, #48cfad)",
                border: "2px solid rgba(255,255,255,0.2)",
                borderRadius: 12,
                color: "#fff",
                fontWeight: 800,
                fontSize: 16,
                cursor: loading || pitch.trim().length < 10 ? "not-allowed" : "pointer",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 10,
                transition: "all 0.2s",
                boxShadow: "0 4px 14px rgba(108,99,255,0.35)",
              }}
            >
              {loading ? (
                <>
                  <div
                    style={{
                      width: 20,
                      height: 20,
                      border: "2px solid rgba(255,255,255,0.3)",
                      borderTopColor: "#fff",
                      borderRadius: "50%",
                      animation: "spin 0.8s linear infinite",
                    }}
                  />
                  Analyzing…
                </>
              ) : (
                <>🔍 Analyze Match</>
              )}
            </button>
            {error && (
              <div
                style={{
                  marginTop: 10, fontSize: 13, color: "#e74c3c",
                  background: "rgba(231,76,60,0.1)", padding: "8px 12px", borderRadius: 8,
                }}
              >
                {error}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT PANEL — Results */}
        <div>
          {!result && !loading && (
            <div
              style={{
                height: "100%", display: "flex", alignItems: "center",
                justifyContent: "center", flexDirection: "column",
                gap: 16, color: "rgba(255,255,255,0.2)",
                textAlign: "center", padding: 40,
              }}
            >
              <div style={{ fontSize: 64 }}>🧠</div>
              <div style={{ fontSize: 18, fontWeight: 700, color: "rgba(255,255,255,0.35)" }}>
                AI Analysis Ready
              </div>
              <div style={{ fontSize: 14, color: "rgba(255,255,255,0.2)", maxWidth: 280, lineHeight: 1.7 }}>
                Write your pitch on the left, then click Analyze Match to see how semantically
                aligned it is with the project requirements.
              </div>
              <div style={{ marginTop: 12, display: "flex", flexDirection: "column", gap: 8, width: "100%", maxWidth: 300 }}>
                {[
                  "Technical Relevance · 30%",
                  "Problem Understanding · 25%",
                  "Experience Signals · 20%",
                  "Communication Quality · 15%",
                  "Value Proposition · 10%",
                ].map((d) => (
                  <div
                    key={d}
                    style={{
                      padding: "8px 14px", background: "rgba(255,255,255,0.03)",
                      borderRadius: 8, fontSize: 12, color: "rgba(255,255,255,0.25)",
                      border: "1px solid rgba(255,255,255,0.05)",
                    }}
                  >
                    {d}
                  </div>
                ))}
              </div>
            </div>
          )}

          {loading && (
            <div
              style={{
                height: "100%", display: "flex", alignItems: "center",
                justifyContent: "center", flexDirection: "column", gap: 24,
              }}
            >
              <div style={{ position: "relative", width: 100, height: 100 }}>
                <div style={{ position: "absolute", inset: 0, border: "3px solid rgba(108,99,255,0.2)", borderRadius: "50%" }} />
                <div
                  style={{
                    position: "absolute", inset: 0,
                    border: "3px solid transparent", borderTopColor: "#6c63ff",
                    borderRadius: "50%", animation: "spin 1s linear infinite",
                  }}
                />
                <div
                  style={{
                    position: "absolute", inset: 12,
                    border: "3px solid transparent", borderTopColor: "#48cfad",
                    borderRadius: "50%", animation: "spin 0.7s linear infinite reverse",
                  }}
                />
                <div
                  style={{
                    position: "absolute", inset: 0,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    fontSize: 28,
                  }}
                >
                  🧠
                </div>
              </div>
              <div style={{ textAlign: "center" }}>
                <div style={{ fontWeight: 800, fontSize: 18, marginBottom: 8 }}>Analyzing Your Pitch</div>
                {[
                  "Reading project requirements…",
                  "Extracting semantic signals…",
                  "Scoring 5 dimensions…",
                  "Generating feedback…",
                ].map((t) => (
                  <div key={t} style={{ fontSize: 13, color: "rgba(255,255,255,0.4)", padding: "3px 0" }}>
                    {t}
                  </div>
                ))}
              </div>
            </div>
          )}

          {result && (
            <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>

              {/* Overall Score */}
              <div
                style={{
                  background: `linear-gradient(135deg, ${SCORE_COLOR(result.overallScore)}18, rgba(108,99,255,0.1))`,
                  borderRadius: 18, padding: 24,
                  border: `1px solid ${SCORE_COLOR(result.overallScore)}33`,
                  display: "flex", alignItems: "center", gap: 24,
                }}
              >
                <RadialScore score={result.overallScore} size={110} />
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 22, fontWeight: 900, color: SCORE_COLOR(result.overallScore), marginBottom: 4 }}>
                    {SCORE_LABEL(result.overallScore)}
                  </div>
                  <div style={{ fontSize: 14, color: "rgba(255,255,255,0.7)", lineHeight: 1.6, marginBottom: 14 }}>
                    {result.verdict}
                  </div>
                  <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                    <span
                      style={{
                        fontSize: 12, padding: "4px 12px", borderRadius: 20,
                        background: `${(likeColor[result.hiringLikelihood] || "#fff")}22`,
                        color: likeColor[result.hiringLikelihood] || "#fff",
                        fontWeight: 700,
                        border: `1px solid ${(likeColor[result.hiringLikelihood] || "#fff")}44`,
                      }}
                    >
                      🎯 Hiring: {result.hiringLikelihood}
                    </span>
                    <span
                      style={{
                        fontSize: 12, padding: "4px 12px", borderRadius: 20,
                        background: `${(toneColor[result.toneAnalysis] || "#fff")}22`,
                        color: toneColor[result.toneAnalysis] || "#fff",
                        fontWeight: 700,
                        border: `1px solid ${(toneColor[result.toneAnalysis] || "#fff")}44`,
                      }}
                    >
                      🎤 Tone: {result.toneAnalysis}
                    </span>
                  </div>
                </div>
              </div>

              {/* Dimension Breakdown */}
              <div
                style={{
                  background: "rgba(255,255,255,0.04)", borderRadius: 18,
                  padding: 22, border: "1px solid rgba(255,255,255,0.08)",
                }}
              >
                <div
                  style={{
                    fontSize: 12, fontWeight: 700, color: "rgba(255,255,255,0.4)",
                    letterSpacing: 1, marginBottom: 16, textTransform: "uppercase",
                  }}
                >
                  📊 Dimension Analysis
                </div>
                {result.dimensions.map((d, i) => (
                  <div key={d.name} style={{ marginBottom: 18 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                      <div style={{ fontWeight: 700, fontSize: 14 }}>{d.name}</div>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <span style={{ fontSize: 11, color: "rgba(255,255,255,0.35)" }}>weight {d.weight}%</span>
                        <span style={{ fontWeight: 900, fontSize: 15, color: SCORE_COLOR(d.score) }}>{d.score}</span>
                      </div>
                    </div>
                    <AnimatedBar value={d.score} color={SCORE_COLOR(d.score)} delay={i * 0.1} />
                    <div style={{ fontSize: 12, color: "rgba(255,255,255,0.5)", marginTop: 7, lineHeight: 1.65 }}>
                      {d.explanation}
                    </div>
                  </div>
                ))}
              </div>

              {/* Matches & Gaps */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
                <div
                  style={{
                    background: "rgba(106,176,76,0.06)", borderRadius: 14,
                    padding: 18, border: "1px solid rgba(106,176,76,0.15)",
                  }}
                >
                  <div
                    style={{
                      fontSize: 12, fontWeight: 700, color: "#6ab04c",
                      marginBottom: 12, textTransform: "uppercase", letterSpacing: 1,
                    }}
                  >
                    ✅ Key Matches
                  </div>
                  {result.keyMatches?.length > 0 ? (
                    result.keyMatches.map((m, i) => (
                      <div
                        key={i}
                        style={{
                          fontSize: 12, color: "rgba(255,255,255,0.65)",
                          padding: "6px 0", borderBottom: "1px solid rgba(255,255,255,0.05)",
                          lineHeight: 1.55,
                        }}
                      >
                        • {m}
                      </div>
                    ))
                  ) : (
                    <div style={{ fontSize: 12, color: "rgba(255,255,255,0.3)" }}>No clear matches found</div>
                  )}
                </div>
                <div
                  style={{
                    background: "rgba(231,76,60,0.05)", borderRadius: 14,
                    padding: 18, border: "1px solid rgba(231,76,60,0.15)",
                  }}
                >
                  <div
                    style={{
                      fontSize: 12, fontWeight: 700, color: "#e74c3c",
                      marginBottom: 12, textTransform: "uppercase", letterSpacing: 1,
                    }}
                  >
                    ⚠️ Key Gaps
                  </div>
                  {result.keyGaps?.length > 0 ? (
                    result.keyGaps.map((g, i) => (
                      <div
                        key={i}
                        style={{
                          fontSize: 12, color: "rgba(255,255,255,0.65)",
                          padding: "6px 0", borderBottom: "1px solid rgba(255,255,255,0.05)",
                          lineHeight: 1.55,
                        }}
                      >
                        • {g}
                      </div>
                    ))
                  ) : (
                    <div style={{ fontSize: 12, color: "#6ab04c" }}>No significant gaps!</div>
                  )}
                </div>
              </div>

              {/* Red Flags */}
              {result.redFlags?.length > 0 && (
                <div
                  style={{
                    background: "rgba(225,112,85,0.07)", borderRadius: 14,
                    padding: 18, border: "1px solid rgba(225,112,85,0.2)",
                  }}
                >
                  <div
                    style={{
                      fontSize: 12, fontWeight: 700, color: "#e17055",
                      marginBottom: 10, textTransform: "uppercase", letterSpacing: 1,
                    }}
                  >
                    🚩 Red Flags
                  </div>
                  {result.redFlags.map((f, i) => (
                    <div key={i} style={{ fontSize: 13, color: "rgba(255,255,255,0.65)", padding: "5px 0", lineHeight: 1.6 }}>
                      • {f}
                    </div>
                  ))}
                </div>
              )}

              {/* Improvement Tips */}
              <div
                style={{
                  background: "linear-gradient(135deg, rgba(108,99,255,0.1), rgba(72,207,173,0.08))",
                  borderRadius: 14, padding: 20, border: "1px solid rgba(108,99,255,0.2)",
                }}
              >
                <div
                  style={{
                    fontSize: 12, fontWeight: 700, color: "#a29bfe",
                    marginBottom: 10, textTransform: "uppercase", letterSpacing: 1,
                  }}
                >
                  💡 How to Improve This Pitch
                </div>
                <div style={{ fontSize: 13, color: "rgba(255,255,255,0.7)", lineHeight: 1.75 }}>
                  {result.improvement}
                </div>
              </div>

            </div>
          )}
        </div>
      </div>
    </div>
  );
}
