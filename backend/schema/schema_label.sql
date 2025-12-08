CREATE TYPE sentiment_level AS ENUM (
    'very_positive',
    'positive',
    'neutral',
    'negative',
    'very_negative'
);
CREATE TABLE label (
    id SERIAL PRIMARY KEY,
    number INT,
    image_name VARCHAR(255),
    image_path TEXT,
    cleaned_image_path TEXT,
    text_ocr TEXT,
    text_corrected TEXT,
    overall_sentiment sentiment_level
)