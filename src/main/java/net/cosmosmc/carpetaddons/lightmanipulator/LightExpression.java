package net.cosmosmc.carpetaddons.lightmanipulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A small math expression language modelled after WorldEdit's expression syntax.
 * <p>
 * Supported:
 * <ul>
 *     <li>numbers ({@code 1}, {@code 0.5}, {@code 1e3}), constants {@code pi}, {@code e}, {@code true}, {@code false}</li>
 *     <li>variables {@code x}, {@code y}, {@code z} and {@code cur} (the current light level of the edited layer)</li>
 *     <li>operators by precedence: {@code ?:}, {@code ||}, {@code &&}, {@code == !=}, {@code < <= > >=},
 *     {@code + -}, {@code * / %}, unary {@code - + !}, {@code ^} / {@code **} (power, right associative)</li>
 *     <li>functions: abs, sqrt, cbrt, sin, cos, tan, asin, acos, atan, atan2, sinh, cosh, tanh, exp, ln, log,
 *     log10, floor, ceil, round, rint, min, max, pow, clamp, hypot, random</li>
 * </ul>
 * The result is interpreted as a light level: it is rounded and clamped to 0..15.
 * A negative or NaN result leaves the light level unchanged.
 */
public final class LightExpression {
    public static final int VAR_X = 0;
    public static final int VAR_Y = 1;
    public static final int VAR_Z = 2;
    public static final int VAR_CUR = 3;
    public static final int VAR_COUNT = 4;

    private final String source;
    private final Node root;

    private LightExpression(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public String source() {
        return source;
    }

    /**
     * @param vars variable values, indexed by the {@code VAR_*} constants
     */
    public double evaluate(double[] vars) {
        return root.eval(vars);
    }

    public static LightExpression parse(String source) throws ParseException {
        Parser parser = new Parser(source);
        Node node = parser.parseTernary();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw parser.error("Unexpected '" + parser.peekChar() + "'");
        }
        return new LightExpression(source, node);
    }

    public static final class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    private interface Node {
        double eval(double[] vars);
    }

    private static boolean truthy(double v) {
        return v != 0 && !Double.isNaN(v);
    }

    private static double bool(boolean b) {
        return b ? 1 : 0;
    }

    private static final class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        ParseException error(String message) {
            return new ParseException(message + " at position " + (pos + 1) + " in '" + src + "'");
        }

        boolean atEnd() {
            return pos >= src.length();
        }

        char peekChar() {
            return src.charAt(pos);
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(peekChar())) pos++;
        }

        boolean accept(String token) {
            skipWhitespace();
            if (src.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        void expect(String token) throws ParseException {
            if (!accept(token)) {
                throw error("Expected '" + token + "'");
            }
        }

        Node parseTernary() throws ParseException {
            Node cond = parseOr();
            if (accept("?")) {
                Node a = parseTernary();
                expect(":");
                Node b = parseTernary();
                return v -> truthy(cond.eval(v)) ? a.eval(v) : b.eval(v);
            }
            return cond;
        }

        Node parseOr() throws ParseException {
            Node left = parseAnd();
            while (accept("||")) {
                Node l = left, r = parseAnd();
                left = v -> bool(truthy(l.eval(v)) || truthy(r.eval(v)));
            }
            return left;
        }

        Node parseAnd() throws ParseException {
            Node left = parseEquality();
            while (accept("&&")) {
                Node l = left, r = parseEquality();
                left = v -> bool(truthy(l.eval(v)) && truthy(r.eval(v)));
            }
            return left;
        }

        Node parseEquality() throws ParseException {
            Node left = parseComparison();
            while (true) {
                Node l = left;
                if (accept("==")) {
                    Node r = parseComparison();
                    left = v -> bool(l.eval(v) == r.eval(v));
                } else if (accept("!=")) {
                    Node r = parseComparison();
                    left = v -> bool(l.eval(v) != r.eval(v));
                } else {
                    return left;
                }
            }
        }

        Node parseComparison() throws ParseException {
            Node left = parseAdditive();
            while (true) {
                Node l = left;
                if (accept("<=")) {
                    Node r = parseAdditive();
                    left = v -> bool(l.eval(v) <= r.eval(v));
                } else if (accept(">=")) {
                    Node r = parseAdditive();
                    left = v -> bool(l.eval(v) >= r.eval(v));
                } else if (accept("<")) {
                    Node r = parseAdditive();
                    left = v -> bool(l.eval(v) < r.eval(v));
                } else if (accept(">")) {
                    Node r = parseAdditive();
                    left = v -> bool(l.eval(v) > r.eval(v));
                } else {
                    return left;
                }
            }
        }

        Node parseAdditive() throws ParseException {
            Node left = parseMultiplicative();
            while (true) {
                Node l = left;
                if (accept("+")) {
                    Node r = parseMultiplicative();
                    left = v -> l.eval(v) + r.eval(v);
                } else if (accept("-")) {
                    Node r = parseMultiplicative();
                    left = v -> l.eval(v) - r.eval(v);
                } else {
                    return left;
                }
            }
        }

        Node parseMultiplicative() throws ParseException {
            Node left = parseUnary();
            while (true) {
                Node l = left;
                skipWhitespace();
                // don't mistake '**' (power) for multiplication
                if (src.startsWith("**", pos)) {
                    return left;
                }
                if (accept("*")) {
                    Node r = parseUnary();
                    left = v -> l.eval(v) * r.eval(v);
                } else if (accept("/")) {
                    Node r = parseUnary();
                    left = v -> l.eval(v) / r.eval(v);
                } else if (accept("%")) {
                    Node r = parseUnary();
                    left = v -> l.eval(v) % r.eval(v);
                } else {
                    return left;
                }
            }
        }

        Node parseUnary() throws ParseException {
            skipWhitespace();
            if (src.startsWith("!=", pos)) {
                throw error("Unexpected '!='");
            }
            if (accept("-")) {
                Node n = parseUnary();
                return v -> -n.eval(v);
            }
            if (accept("+")) {
                return parseUnary();
            }
            if (accept("!")) {
                Node n = parseUnary();
                return v -> bool(!truthy(n.eval(v)));
            }
            return parsePower();
        }

        Node parsePower() throws ParseException {
            Node base = parsePrimary();
            if (accept("^") || accept("**")) {
                // right associative, binds tighter than unary minus on the left: -2^2 == -4
                Node exp = parseUnary();
                return v -> Math.pow(base.eval(v), exp.eval(v));
            }
            return base;
        }

        Node parsePrimary() throws ParseException {
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of expression");
            }
            char c = peekChar();
            if (c == '(') {
                pos++;
                Node inner = parseTernary();
                expect(")");
                return inner;
            }
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (!atEnd() && (Character.isLetterOrDigit(peekChar()) || peekChar() == '_')) pos++;
                String name = src.substring(start, pos).toLowerCase(Locale.ROOT);
                if (accept("(")) {
                    List<Node> args = new ArrayList<>();
                    if (!accept(")")) {
                        do {
                            args.add(parseTernary());
                        } while (accept(","));
                        expect(")");
                    }
                    return function(name, args);
                }
                return identifier(name);
            }
            throw error("Unexpected '" + c + "'");
        }

        Node parseNumber() throws ParseException {
            int start = pos;
            while (!atEnd() && (Character.isDigit(peekChar()) || peekChar() == '.')) pos++;
            if (!atEnd() && (peekChar() == 'e' || peekChar() == 'E')) {
                int save = pos;
                pos++;
                if (!atEnd() && (peekChar() == '+' || peekChar() == '-')) pos++;
                if (!atEnd() && Character.isDigit(peekChar())) {
                    while (!atEnd() && Character.isDigit(peekChar())) pos++;
                } else {
                    pos = save;
                }
            }
            String text = src.substring(start, pos);
            try {
                double value = Double.parseDouble(text);
                return v -> value;
            } catch (NumberFormatException e) {
                pos = start;
                throw error("Invalid number '" + text + "'");
            }
        }

        Node identifier(String name) throws ParseException {
            return switch (name) {
                case "x" -> v -> v[VAR_X];
                case "y" -> v -> v[VAR_Y];
                case "z" -> v -> v[VAR_Z];
                case "cur", "light" -> v -> v[VAR_CUR];
                case "pi" -> v -> Math.PI;
                case "e" -> v -> Math.E;
                case "true" -> v -> 1;
                case "false" -> v -> 0;
                default -> throw error("Unknown variable '" + name + "'");
            };
        }

        Node function(String name, List<Node> args) throws ParseException {
            int n = args.size();
            return switch (name) {
                case "abs" -> unary(name, args, Math::abs);
                case "sqrt" -> unary(name, args, Math::sqrt);
                case "cbrt" -> unary(name, args, Math::cbrt);
                case "sin" -> unary(name, args, Math::sin);
                case "cos" -> unary(name, args, Math::cos);
                case "tan" -> unary(name, args, Math::tan);
                case "asin" -> unary(name, args, Math::asin);
                case "acos" -> unary(name, args, Math::acos);
                case "atan" -> unary(name, args, Math::atan);
                case "sinh" -> unary(name, args, Math::sinh);
                case "cosh" -> unary(name, args, Math::cosh);
                case "tanh" -> unary(name, args, Math::tanh);
                case "exp" -> unary(name, args, Math::exp);
                case "ln" -> unary(name, args, Math::log);
                case "log10" -> unary(name, args, Math::log10);
                case "floor" -> unary(name, args, Math::floor);
                case "ceil" -> unary(name, args, Math::ceil);
                case "round" -> unary(name, args, a -> (double) Math.round(a));
                case "rint" -> unary(name, args, Math::rint);
                case "log" -> {
                    if (n == 1) yield unary(name, args, Math::log);
                    // log(value, base)
                    yield binary(name, args, (a, b) -> Math.log(a) / Math.log(b));
                }
                case "atan2" -> binary(name, args, Math::atan2);
                case "pow" -> binary(name, args, Math::pow);
                case "hypot" -> binary(name, args, Math::hypot);
                case "min", "max" -> {
                    if (n < 1) throw error(name + "() needs at least one argument");
                    Node[] a = args.toArray(new Node[0]);
                    boolean isMin = name.equals("min");
                    yield v -> {
                        double r = a[0].eval(v);
                        for (int i = 1; i < a.length; i++) {
                            double o = a[i].eval(v);
                            r = isMin ? Math.min(r, o) : Math.max(r, o);
                        }
                        return r;
                    };
                }
                case "clamp" -> {
                    if (n != 3) throw error("clamp() needs 3 arguments (value, min, max)");
                    Node a = args.get(0), lo = args.get(1), hi = args.get(2);
                    yield v -> Math.max(lo.eval(v), Math.min(hi.eval(v), a.eval(v)));
                }
                case "random" -> {
                    if (n != 0) throw error("random() takes no arguments");
                    yield v -> ThreadLocalRandom.current().nextDouble();
                }
                default -> throw error("Unknown function '" + name + "'");
            };
        }

        private interface D1 { double apply(double a); }
        private interface D2 { double apply(double a, double b); }

        Node unary(String name, List<Node> args, D1 f) throws ParseException {
            if (args.size() != 1) throw error(name + "() needs 1 argument");
            Node a = args.getFirst();
            return v -> f.apply(a.eval(v));
        }

        Node binary(String name, List<Node> args, D2 f) throws ParseException {
            if (args.size() != 2) throw error(name + "() needs 2 arguments");
            Node a = args.get(0), b = args.get(1);
            return v -> f.apply(a.eval(v), b.eval(v));
        }
    }
}
