CREATE TABLE IF NOT EXISTS event_publication
(
    id               TEST NOT NULL PRIMARY KEY,
    listener_id      TEXT NOT NULL INDEX OFF,
    event_type       TEXT NOT NULL INDEX OFF,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL INDEX OFF,
    completion_date  TIMESTAMP WITH TIME ZONE,
    );