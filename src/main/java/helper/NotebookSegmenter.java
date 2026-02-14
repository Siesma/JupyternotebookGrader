package helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NotebookSegmenter {

    private final JSONObject notebook;

    public NotebookSegmenter(JSONObject notebook) {
        this.notebook = notebook;
    }

    /**
     * @return [a, mLen, b, mCount] such that the notebook fits a(m+)b
     */
    public int[] determineBoundedAreas() {
        int[] output = new int[4];

        JSONArray cells = notebook.optJSONArray("cells");
        if (cells == null) return output;

        int n = cells.length();
        if (n == 0) return output;

        long[] sig = new long[n];
        for (int i = 0; i < n; i++) {
            sig[i] = roleSignature(cells.optJSONObject(i));
        }

        Candidate best = null;

        double blockMatchThreshold = 0.80;

        for (int mLen = 1; mLen <= n / 2; mLen++) {

            for (int a = 0; a + 2 * mLen <= n; a++) {

                int blocksTotal = (n - a) / mLen;
                if (blocksTotal < 2) continue;

                long[] pattern = majorityPattern(sig, a, mLen, blocksTotal);

                int goodBlocks = 0;
                for (int k = 0; k < blocksTotal; k++) {
                    int start = a + k * mLen;
                    double frac = blockMatchFraction(sig, start, pattern, mLen);
                    if (frac >= blockMatchThreshold) {
                        goodBlocks++;
                    } else {
                        break;
                    }
                }

                if (goodBlocks < 2) continue;

                int covered = goodBlocks * mLen;
                int b = n - (a + covered);

                Candidate cand = new Candidate(a, mLen, b, goodBlocks);

                if (best == null || cand.betterThan(best)) {
                    best = cand;
                }
            }
        }

        if (best == null) {
            // fallback
            output[0] = n;
            output[1] = 0;
            output[2] = 0;
            output[3] = 0;
            return output;
        }

        output[0] = best.a;
        output[1] = best.mLen;
        output[2] = best.b;
        output[3] = best.mCount;
        return output;
    }

    private static class Candidate {
        final int a, mLen, b, mCount;

        Candidate(int a, int mLen, int b, int mCount) {
            this.a = a;
            this.mLen = mLen;
            this.b = b;
            this.mCount = mCount;
        }

        boolean betterThan(Candidate other) {
            if (this.mCount != other.mCount) return this.mCount > other.mCount;
            if (this.b != other.b) return this.b < other.b;
            if (this.a != other.a) return this.a < other.a;
            return this.mLen < other.mLen;
        }
    }

    private static long[] majorityPattern(long[] sig, int a, int mLen, int blocksTotal) {
        long[] pattern = new long[mLen];

        for (int t = 0; t < mLen; t++) {
            Map<Long, Integer> freq = new HashMap<>();
            long bestVal = 0;
            int bestCount = -1;

            for (int k = 0; k < blocksTotal; k++) {
                long v = sig[a + k * mLen + t];
                int c = freq.getOrDefault(v, 0) + 1;
                freq.put(v, c);
                if (c > bestCount) {
                    bestCount = c;
                    bestVal = v;
                }
            }
            pattern[t] = bestVal;
        }

        return pattern;
    }

    private static double blockMatchFraction(long[] sig, int start, long[] pattern, int mLen) {
        int match = 0;
        for (int i = 0; i < mLen; i++) {
            if (sig[start + i] == pattern[i]) match++;
        }
        return (double) match / (double) mLen;
    }

    /**
     * Stable signature based on ROLE-ish metadata only.
     * <p>
     * Encodes:
     * - cell_type (markdown/code/raw)
     * - metadata flags: deletable/editable
     * - presence bits: student_id, exercise_id, score_cell, comment_cell, notebook_id
     */
    private static long roleSignature(JSONObject cell) {
        if (cell == null) return 0;

        String type = cell.optString("cell_type", "");
        int typeCode = switch (type) {
            case "markdown" -> 1;
            case "code" -> 2;
            case "raw" -> 3;
            case null, default -> 0;
        };

        JSONObject meta = cell.optJSONObject("metadata");

        boolean deletable = true;
        boolean editable = true;

        boolean hasStudentId = false;
        boolean hasExerciseId = false;
        boolean hasScoreCell = false;
        boolean hasCommentCell = false;
        boolean hasNotebookId = false;

        /*
        Is this safe to assume to be present/syntactic?
         */
        if (meta != null) {
            deletable = meta.optBoolean("deletable", true);
            editable = meta.optBoolean("editable", true);

            hasStudentId = meta.has("student_id");
            hasExerciseId = meta.has("exercise_id");
            hasScoreCell = meta.has("score_cell");
            hasCommentCell = meta.has("comment_cell");
            hasNotebookId = meta.has("notebook_id");
        }

        long bits = 0;
        bits |= (typeCode & 0b111);
        bits |= (deletable ? 1L : 0L) << 3;
        bits |= (editable ? 1L : 0L) << 4;
        bits |= (hasStudentId ? 1L : 0L) << 5;
        bits |= (hasExerciseId ? 1L : 0L) << 6;
        bits |= (hasScoreCell ? 1L : 0L) << 7;
        bits |= (hasCommentCell ? 1L : 0L) << 8;
        bits |= (hasNotebookId ? 1L : 0L) << 9;

        return bits;
    }

}
