package com.DGSD.Teexter.UI.Recipient;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

public class RecipientEditorTokenizer implements MultiAutoCompleteTextView.Tokenizer {

	/**
	 * Returns the start of the token that ends at offset <code>cursor</code>
	 * within <code>text</code>. It is a method from the
	 * MultiAutoCompleteTextView.Tokenizer interface.
	 */
	public int findTokenStart(CharSequence text, int cursor) {
		int i = cursor;
		char c;

		while (i > 0 && (c = text.charAt(i - 1)) != ',' && c != ';') {
			i--;
		}
		while (i < cursor && text.charAt(i) == ' ') {
			i++;
		}

		return i;
	}

	/**
	 * Returns the end of the token (minus trailing punctuation) that begins at
	 * offset <code>cursor</code> within <code>text</code>. It is a method from
	 * the MultiAutoCompleteTextView.Tokenizer interface.
	 */
	public int findTokenEnd(CharSequence text, int cursor) {
		int i = cursor;
		int len = text.length();
		char c;

		while (i < len) {
			if ((c = text.charAt(i)) == ',' || c == ';') {
				return i;
			} else {
				i++;
			}
		}

		return len;
	}

	/**
	 * Returns <code>text</code>, modified, if necessary, to ensure that it ends
	 * with a token terminator (for example a space or comma). It is a method
	 * from the MultiAutoCompleteTextView.Tokenizer interface.
	 */
	public CharSequence terminateToken(CharSequence text) {
		int i = text.length();

		while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}

		char c;
		if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == ';')) {
			return text;
		} else {
			String separator = ", ";
			if (text instanceof Spanned) {
				SpannableString sp = new SpannableString(text + separator);
				TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
				return sp;
			} else {
				return text + separator;
			}
		}
	}
}
