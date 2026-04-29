# SonarQube Evidence Checklist

Use this file as your defense checklist to capture the required evidence.

## A) Initial state screenshots (before improvements)

- [ ] Frontend Sonar dashboard (`matchfreelance-frontend`)
- [ ] Candidature Sonar dashboard (`matchfreelance-candidature`)
- [ ] Contract Sonar dashboard (`matchfreelance-contract`)
- [ ] Quality issues list (bugs/code smells)
- [ ] Test coverage summary

## B) Improvement cycle

- [ ] Add/refactor tests in your owned frontend module
- [ ] Add/refactor tests in candidature module
- [ ] Add/refactor tests in contract module
- [ ] Re-run CI pipelines
- [ ] Re-run Sonar analysis for all 3 projects

## C) Final state screenshots (after improvement)

- [ ] Frontend updated quality/coverage
- [ ] Candidature updated quality/coverage
- [ ] Contract updated quality/coverage
- [ ] Before/after comparison slide ready

## D) CI/CD proof

- [ ] CI success screenshot for each component
- [ ] Auto-triggered CD screenshot for each component
- [ ] Docker Hub pushed tags screenshot
- [ ] Kubernetes rollout success screenshot

## E) Monitoring proof

- [ ] Prometheus targets healthy
- [ ] Grafana dashboard with frontend + backend metrics
- [ ] Alert rule visible in Alertmanager
