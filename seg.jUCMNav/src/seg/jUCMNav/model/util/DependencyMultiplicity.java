package seg.jUCMNav.model.util;

/**
 * Parsing, validation and display formatting for dependency multiplicities.
 * 
 * <p>
 * A multiplicity constrains one end of a {@code grl.Dependency} and is stored, per end, as a
 * plain string attribute on the dependency definition ({@code grl.Dependency#getSrcMultiplicity()}
 * / {@code getDestMultiplicity()}). The canonical form is {@code x..y} where each bound is either
 * a non-negative integer or {@code *} (unconstrained in that direction); {@code y >= x} must hold
 * when both bounds are integers. An empty string means the end has no multiplicity.
 * </p>
 * 
 * <p>
 * The stored value may also contain surrounding brackets and stray whitespace
 * ({@code " [1 .. *] "}); {@link #toDisplay(String)} renders it back as {@code [1..*]}.
 * </p>
 * 
 * @author skuly
 */
public final class DependencyMultiplicity {

    private DependencyMultiplicity() {
        // utility class
    }

    /**
     * @return {@code false} if {@code value} is {@code null} or does not denote a valid
     *         multiplicity; an empty string is valid (no multiplicity on that end)
     */
    public static boolean isValid(String value) {
        if (value == null)
            return false;
        String s = normalize(value);
        if (s.length() == 0)
            return true;

        int separator = s.indexOf(".."); //$NON-NLS-1$
        if (separator <= 0 || separator + 2 >= s.length())
            return false;
        if (s.indexOf("..", separator + 2) >= 0) //$NON-NLS-1$
            return false;

        Integer lower = parseBound(s.substring(0, separator));
        Integer upper = parseBound(s.substring(separator + 2));
        if (lower == null || upper == null)
            return false;

        return lower == UNBOUNDED || upper == UNBOUNDED || lower.intValue() <= upper.intValue();
    }

    /** Sentinel for an unbounded ({@code *}) bound, never a valid non-negative integer. */
    private static final Integer UNBOUNDED = Integer.valueOf(-1);

    /**
     * Trims whitespace, strips one pair of surrounding square brackets, and collapses internal
     * whitespace around the {@code ..} separator.
     */
    private static String normalize(String value) {
        String s = value.trim();
        if (s.length() >= 2 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']')
            s = s.substring(1, s.length() - 1).trim();
        return s.replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Parses a single bound: {@code *} (unbounded) returns {@link #UNBOUNDED}, a valid
     * non-negative integer returns its value, anything else returns {@code null}.
     */
    private static Integer parseBound(String bound) {
        if (bound.equals("*")) //$NON-NLS-1$
            return UNBOUNDED;
        try {
            int value = Integer.parseInt(bound);
            return value >= 0 ? Integer.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Renders the stored value for the diagram: {@code [x..y]} with normalized spacing, or the
     * empty string when the value is empty/null (no multiplicity).
     */
    public static String toDisplay(String value) {
        if (value == null || normalize(value).length() == 0)
            return ""; //$NON-NLS-1$
        return "[" + normalize(value) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @return the canonical stored form ({@code x..y}) of the value, or {@code ""} for null/empty
     *         after normalization. Invalid input is returned unchanged.
     */
    public static String normalizeStored(String value) {
        if (value == null)
            return ""; //$NON-NLS-1$
        String s = normalize(value);
        if (s.length() == 0)
            return ""; //$NON-NLS-1$
        if (!isValid(value))
            return value.trim();
        return s;
    }
}