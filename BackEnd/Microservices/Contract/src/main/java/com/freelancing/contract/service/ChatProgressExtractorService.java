package com.freelancing.contract.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts project progress (0-100%) from freelancer chat messages using rule-based parsing.
 * Recognizes: explicit percentages ("50%", "backend 40%"), component + phrase ("devops nearly done",
 * "frontend halfway", "backend just starting"), and combines multiple parts into one overall percentage
 * (e.g. average of components). Used to update the contract progress bar.
 */
@Service
public class ChatProgressExtractorService {

    private static final String COMPONENT_WORDS =
            "backend|back end|back-end|frontend|front end|front-end|api|design|ui|ux|database|db|testing|tests|"
            + "integration|devops|mobile|web|server|client|auth|login|dashboard|admin|docs|documentation|deploy|deployment|"
            + "styling|css|rest|graphql|data layer|services|core|feature|module|task|phase|sprint|milestone|fullstack|full stack|"
            + "first phase|second phase|third phase|part one|part two|step one|step two";

    // Any number + % (e.g. "50%", "update: 40%") - used as fallback
    private static final Pattern ANY_PERCENT = Pattern.compile("(\\d{1,3})\\s*%");

    // Overall progress: many trigger words before a number and %
    private static final Pattern OVERALL_PERCENT = Pattern.compile(
            "(?:progress|update|status|at|currently|we're|we are|i'm at|i am at|standing at|around|about|roughly|approximately|"
            + "sitting at|running at|so far|right now|project is|task is|work is|done with|like|maybe|progress is|"
            + "overall|total|estimate[d]?|roughly)?\\s*(\\d{1,3})\\s*%",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERALL_PERCENT_WORD = Pattern.compile(
            "(?:progress|at|currently|update|status)?\\s*(\\d{1,3})\\s*(?:percent|per cent|pc)",
            Pattern.CASE_INSENSITIVE);

    // Component + percent: "backend 40%", "frontend is at 80%"
    private static final Pattern COMPONENT_PERCENT = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|at|:|=|we're|we are)?\\s*(\\d{1,3})\\s*%",
            Pattern.CASE_INSENSITIVE);

    // Component + phrase: "devops nearly done", "backend just starting", "frontend is halfway"
    // Check ALMOST_DONE before FULLY_DONE so "mostly/almost/nearly finished" -> 90, not 100
    private static final Pattern COMPONENT_ALMOST_DONE = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:almost|nearly|mostly)\\s+(?:done|finished|complete|there)\\b",
            Pattern.CASE_INSENSITIVE);
    // "finished" only when not preceded by " mostly "/" almost "/" nearly " (so we don't double-count with ALMOST_DONE)
    private static final Pattern COMPONENT_FULLY_DONE = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:done|complete|wrapped|(?<! mostly )(?<! almost )(?<! nearly )finished)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_THREE_QUARTERS = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:three quarters|3/4|75%|three-fourths)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_MORE_THAN_HALF = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:more than half|over half|past halfway|majority done)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_TWO_THIRDS = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:two thirds|2/3|66%)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_HALFWAY = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:halfway|half way|half done|50%|in the middle)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_ONE_THIRD = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:one third|1/3|33%|a third)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_QUARTER = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:a quarter|quarter done|25%|quarter (?:of the )?way)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_JUST_STARTED = Pattern.compile(
            "\\b(" + COMPONENT_WORDS + ")\\s*(?:is|'s|we're|we are)?\\s*(?:just started|just starting|getting started|barely started|just beginning|at the start|early stages|kicked off)\\b",
            Pattern.CASE_INSENSITIVE);
    // "we're just starting backend" / "backend we're just starting"
    private static final Pattern PHRASE_THEN_COMPONENT_START = Pattern.compile(
            "\\b(?:just started|getting started|starting|just starting|barely started)\\s+(?:the\\s+)?(" + COMPONENT_WORDS + ")\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHRASE_THEN_COMPONENT_HALF = Pattern.compile(
            "\\b(?:halfway|half way)\\s+(?:with\\s+)?(?:the\\s+)?(" + COMPONENT_WORDS + ")\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHRASE_THEN_COMPONENT_DONE = Pattern.compile(
            "\\b(?:nearly|almost|mostly)\\s+(?:done\\s+)?(?:finished\\s+)?(?:with\\s+)?(?:the\\s+)?(" + COMPONENT_WORDS + ")\\b",
            Pattern.CASE_INSENSITIVE);

    // "X% done", "X% complete", "X% left", "X% remaining", "only X% left"
    private static final Pattern PERCENT_DONE_LEFT = Pattern.compile(
            "\\b(?:only\\s+)?(\\d{1,3})\\s*%\\s*(?:done|complete|finished|left|remaining|to go|complete[d]?|finished)",
            Pattern.CASE_INSENSITIVE);

    // "X out of Y" or "X/Y" / "X of Y" -> progress = round(100 * X / Y)
    private static final Pattern FRACTION_PROGRESS = Pattern.compile(
            "\\b(\\d{1,3})\\s*(?:out of|/|of)\\s*(\\d{1,3})\\s*(?:tasks?|steps?|parts?|phases?|done|complete)?",
            Pattern.CASE_INSENSITIVE);

    // 100% phrases
    private static final Pattern FULLY_DONE = Pattern.compile(
            "\\b(?:all done|fully done|100%|hundred percent|complete|finished|delivered|wrapped up|all complete|"
            + "everything (?:is )?done|submitted|handed over|ready for review|shipped|released|live|deployed|wrapped|"
            + "closed out|signed off|all set|good to go|that's it|that is it|done with (?:the )?project|project done)\\b",
            Pattern.CASE_INSENSITIVE);

    // High progress phrases (~90%)
    private static final Pattern ALMOST_DONE = Pattern.compile(
            "\\b(?:almost done|almost finished|almost complete|nearly done|nearly finished|nearly there|almost there|"
            + "mostly done|mostly finished|mostly complete|"
            + "just about done|pretty much done|close to done|coming to an end|in the home stretch|final stretch|last leg|"
            + "winding down|just about there|final touches|final polish|one more thing|polishing)\\b",
            Pattern.CASE_INSENSITIVE);

    // ~75%
    private static final Pattern THREE_QUARTERS = Pattern.compile(
            "\\b(?:three quarters|3/4|75%|seventy five percent|three fourths|three-quarters)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MORE_THAN_HALF = Pattern.compile(
            "\\b(?:more than half|over half|past halfway|well over half|majority done|most of it done)\\b",
            Pattern.CASE_INSENSITIVE);

    // ~66%
    private static final Pattern TWO_THIRDS = Pattern.compile(
            "\\b(?:two thirds|2/3|66%|sixty six percent|two-thirds)\\b",
            Pattern.CASE_INSENSITIVE);

    // 50%
    private static final Pattern HALFWAY = Pattern.compile(
            "\\b(?:halfway|half way|half done|50%|fifty percent|in the middle|half (?:the )?project|half (?:of )?it)\\b",
            Pattern.CASE_INSENSITIVE);

    // ~33%
    private static final Pattern ONE_THIRD = Pattern.compile(
            "\\b(?:one third|1/3|33%|thirty three percent|a third|one-third)\\b",
            Pattern.CASE_INSENSITIVE);

    // ~25%
    private static final Pattern QUARTER = Pattern.compile(
            "\\b(?:quarter done|25%|twenty five percent|a quarter|one quarter|quarter (?:of the )?way)\\b",
            Pattern.CASE_INSENSITIVE);

    // Low / just started
    private static final Pattern JUST_STARTED = Pattern.compile(
            "\\b(?:just started|getting started|only started|barely started|at the beginning|early stages|"
            + "kicked off|just begun|in progress|working on it|started (?:today|yesterday)?|not yet|still at the start|"
            + "first steps|initial phase|groundwork)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parses message content and returns an optional progress percentage (0-100) if detected.
     * Only consider messages from the freelancer; this method does not check sender - caller must.
     */
    public Optional<Integer> extractProgressFromMessage(String messageContent) {
        if (messageContent == null || messageContent.isBlank()) {
            return Optional.empty();
        }
        // Normalize: trim, collapse spaces, replace fullwidth percent (％) with %
        String text = messageContent.trim()
                .replace('\uFF05', '%')
                .replaceAll("\\s+", " ");
        if (text.length() < 2) return Optional.empty();

        // 1) Component breakdown first: when multiple parts are mentioned ("frontend done, backend halfway, devops started"),
        //    use their average so a stray "100%" (e.g. "it says 100%") or standalone "finished" doesn't override.
        List<Integer> componentPercents = parseComponentPercents(text);
        componentPercents.addAll(parseComponentPhrasePercents(text));
        if (!componentPercents.isEmpty()) {
            int avg = (int) Math.round(componentPercents.stream().mapToInt(Integer::intValue).average().orElse(0));
            return Optional.of(clamp(avg));
        }

        // 2) Explicit overall percentage
        Integer overall = parseOverallPercent(text);
        if (overall != null) return Optional.of(clamp(overall));

        // 3) "X% done" / "X% left"
        Integer doneLeft = parsePercentDoneOrLeft(text);
        if (doneLeft != null) return Optional.of(clamp(doneLeft));

        // 4) "X out of Y" / "X/Y" (e.g. "3 out of 5 tasks done")
        Integer fraction = parseFractionProgress(text);
        if (fraction != null) return Optional.of(clamp(fraction));

        // 5) Phrases (order: almost before full so "mostly finished" -> 90)
        if (ALMOST_DONE.matcher(text).find()) return Optional.of(90);
        if (FULLY_DONE.matcher(text).find()) return Optional.of(100);
        if (THREE_QUARTERS.matcher(text).find()) return Optional.of(75);
        if (MORE_THAN_HALF.matcher(text).find()) return Optional.of(65);
        if (TWO_THIRDS.matcher(text).find()) return Optional.of(66);
        if (HALFWAY.matcher(text).find()) return Optional.of(50);
        if (ONE_THIRD.matcher(text).find()) return Optional.of(33);
        if (QUARTER.matcher(text).find()) return Optional.of(25);
        if (JUST_STARTED.matcher(text).find()) return Optional.of(10);

        // 6) Fallback: any "NNN%" in the message (e.g. "Quick update 40%" or just "40%")
        Integer any = parseAnyPercent(text);
        if (any != null) return Optional.of(clamp(any));

        return Optional.empty();
    }

    private Integer parseAnyPercent(String text) {
        Matcher m = ANY_PERCENT.matcher(text);
        Integer last = null;
        while (m.find()) last = parseIntSafe(m.group(1));
        return last;
    }

    private Integer parseOverallPercent(String text) {
        Matcher m = OVERALL_PERCENT.matcher(text);
        if (m.find()) return parseIntSafe(m.group(1));
        m = OVERALL_PERCENT_WORD.matcher(text);
        if (m.find()) return parseIntSafe(m.group(1));
        return null;
    }

    private Integer parsePercentDoneOrLeft(String text) {
        Matcher m = PERCENT_DONE_LEFT.matcher(text);
        if (!m.find()) return null;
        Integer value = parseIntSafe(m.group(1));
        if (value == null) return null;
        String segment = text.substring(Math.max(0, m.start() - 10), Math.min(text.length(), m.end() + 10)).toLowerCase();
        // "X% left" or "X% remaining" or "X% to go" -> progress = 100 - X
        if (segment.contains("left") || segment.contains("remaining") || segment.contains("to go")) {
            return 100 - value;
        }
        return value;
    }

    private Integer parseFractionProgress(String text) {
        Matcher m = FRACTION_PROGRESS.matcher(text);
        if (!m.find()) return null;
        Integer num = parseIntSafe(m.group(1));
        Integer den = parseIntSafe(m.group(2));
        if (num == null || den == null || den == 0) return null;
        return (int) Math.round(100.0 * num / den);
    }

    private List<Integer> parseComponentPercents(String text) {
        List<Integer> list = new ArrayList<>();
        Matcher m = COMPONENT_PERCENT.matcher(text);
        while (m.find()) {
            Integer p = parseIntSafe(m.group(2)); // group 1 = component name, group 2 = number
            if (p != null) list.add(p);
        }
        return list;
    }

    /**
     * Parses "component + phrase" and "phrase + component" (e.g. "devops nearly done", "backend just starting",
     * "frontend is halfway", "we're just starting backend"). Splits by comma/semicolon/and so multiple parts
     * in one message are combined into one overall percentage.
     */
    private List<Integer> parseComponentPhrasePercents(String text) {
        List<Integer> list = new ArrayList<>();
        // Split by comma, " and ", " & " so multi-part messages get multiple segments
        String[] segments = text.split("[,;]|\\s+and\\s+|\\s+&\\s+");
        for (String segment : segments) {
            String s = segment.trim();
            if (s.isEmpty()) continue;
            // Collect every component+phrase match in this segment (no continue) so e.g.
            // "front end mostly finished backend halfway devops just started" yields 90, 50, 10 -> avg 50
            if (COMPONENT_ALMOST_DONE.matcher(s).find()) list.add(90);
            if (COMPONENT_FULLY_DONE.matcher(s).find()) list.add(100);
            if (COMPONENT_THREE_QUARTERS.matcher(s).find()) list.add(75);
            if (COMPONENT_MORE_THAN_HALF.matcher(s).find()) list.add(65);
            if (COMPONENT_TWO_THIRDS.matcher(s).find()) list.add(66);
            if (COMPONENT_HALFWAY.matcher(s).find()) list.add(50);
            if (COMPONENT_ONE_THIRD.matcher(s).find()) list.add(33);
            if (COMPONENT_QUARTER.matcher(s).find()) list.add(25);
            if (COMPONENT_JUST_STARTED.matcher(s).find()) list.add(10);
        }
        // "phrase + component" on full text (e.g. "we're just starting backend", "nearly done with devops")
        if (PHRASE_THEN_COMPONENT_START.matcher(text).find()) list.add(10);
        if (PHRASE_THEN_COMPONENT_HALF.matcher(text).find()) list.add(50);
        if (PHRASE_THEN_COMPONENT_DONE.matcher(text).find()) list.add(90);
        return list;
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
