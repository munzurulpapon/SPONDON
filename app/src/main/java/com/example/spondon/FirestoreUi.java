package com.example.spondon;

import android.content.Context;
import android.net.ConnectivityManager;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Date;

/**
 * Small shared helpers for the Firestore-backed screens.
 *
 * Why this exists: DocumentSnapshot.getString() THROWS a RuntimeException when the stored value
 * is not a String (for example a number or a Timestamp typed by hand in the Firebase console).
 * Inside a snapshot listener that exception takes the whole screen down. The readers below never
 * throw, and describe() turns a Firestore failure into a message that tells the admin what
 * actually went wrong (permissions, connectivity, ...) instead of a generic "failed to load".
 */
final class FirestoreUi {

    private FirestoreUi() {
    }

    /** Reads any field as a trimmed String. Returns null when missing, blank or unreadable. */
    static String str(DocumentSnapshot doc, String field) {
        try {
            Object o = doc.get(field, ServerTimestampBehavior.ESTIMATE);
            if (o == null) return null;
            String s = String.valueOf(o).trim();
            return s.isEmpty() || "null".equals(s) ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    /** Same as {@link #str(DocumentSnapshot, String)} but returns {@code fallback} when empty. */
    static String str(DocumentSnapshot doc, String field, String fallback) {
        String s = str(doc, field);
        return s != null ? s : fallback;
    }

    /** Reads a Timestamp / Date / Number field as epoch millis (0 when missing or unreadable). */
    static long millis(DocumentSnapshot doc, String field) {
        try {
            // ESTIMATE: a document that was just written locally has no server time yet.
            Object o = doc.get(field, ServerTimestampBehavior.ESTIMATE);
            if (o instanceof Timestamp) return ((Timestamp) o).toDate().getTime();
            if (o instanceof Date) return ((Date) o).getTime();
            if (o instanceof Number) return ((Number) o).longValue();
        } catch (Exception ignored) {
        }
        return 0L;
    }

    /** Reads a numeric field as int (0 when missing or not a number). */
    static int intOf(DocumentSnapshot doc, String field) {
        try {
            Object o = doc.get(field);
            if (o instanceof Number) return ((Number) o).intValue();
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * True when the device reports an active network. If the state cannot be determined we say
     * "online" so a working connection is never blocked by a false negative.
     */
    @SuppressWarnings("deprecation")
    static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    /** Turns a Firestore / Task failure into a message an admin can act on. */
    static String describe(Exception e) {
        if (e == null) return "Unknown error.";

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            FirebaseFirestoreException.Code code = fe.getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "Permission denied (PERMISSION_DENIED). Firestore security rules are "
                            + "blocking this action. Publish the project's latest firestore.rules "
                            + "and make sure you are signed in with an Admin account.";
                case UNAUTHENTICATED:
                    return "You are signed out (UNAUTHENTICATED). Please log in again.";
                case UNAVAILABLE:
                    return "Cannot reach Firebase (UNAVAILABLE). Check your internet connection "
                            + "and try again.";
                case DEADLINE_EXCEEDED:
                    return "The server took too long to respond (DEADLINE_EXCEEDED). "
                            + "Check your connection and try again.";
                case FAILED_PRECONDITION:
                    return "Firestore rejected the query (FAILED_PRECONDITION): " + fe.getMessage();
                default:
                    return code.name() + ": " + fe.getMessage();
            }
        }

        String m = e.getMessage();
        return m != null && !m.isEmpty() ? m : e.getClass().getSimpleName();
    }
}
