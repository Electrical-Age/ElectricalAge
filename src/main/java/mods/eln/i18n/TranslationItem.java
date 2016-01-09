package mods.eln.i18n;

import java.util.Properties;

class TranslationItem implements Comparable<TranslationItem> {
    private final String key;
    private String text;

    public TranslationItem(String text) {
        this.key = I18N.encodeLangKey(text);
        this.text = text;
    }

    public TranslationItem(String key, String text) {
        this.key = key;
        this.text = text;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public boolean isValid() {
        return key != null;
    }

    public void applyExistingTranslationIfPresent(final Properties existing) {
        if (existing != null) {
            String text = existing.getProperty(key);
            if (text != null) {
                this.text = text;
            }
        }
    }

    @Override
    public int compareTo(TranslationItem other) throws NullPointerException {
        return key.compareTo(other.key);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof TranslationItem && compareTo((TranslationItem) object) == 0 ||
            object instanceof String && key.compareTo((String) object) == 0;
    }

    @Override
    public String toString() {
        return (new StringBuilder(key)).append("=").append(text.replace("\\\"", "\"")).append("\n").toString();
    }
}
