CREATE TABLE IF NOT EXISTS `audits` (
	`id` text PRIMARY KEY NOT NULL,
	`actor` text NOT NULL,
	`action` text NOT NULL,
	`created` integer NOT NULL
);

CREATE TABLE IF NOT EXISTS `blocks` (
	`id` text PRIMARY KEY NOT NULL,
	`owner` text NOT NULL,
	`target` text NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS `one_block` ON `blocks` (`owner`,`target`);
CREATE TABLE IF NOT EXISTS `messages` (
	`id` text PRIMARY KEY NOT NULL,
	`session` text NOT NULL,
	`sender` text NOT NULL,
	`body` text NOT NULL,
	`kind` text DEFAULT 'chat' NOT NULL,
	`created` integer NOT NULL
);

CREATE TABLE IF NOT EXISTS `ratings` (
	`id` text PRIMARY KEY NOT NULL,
	`session` text NOT NULL,
	`speaker` text NOT NULL,
	`listener` text NOT NULL,
	`score` integer NOT NULL,
	`app_score` integer NOT NULL,
	`review` text NOT NULL,
	`created` integer NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS `one_review` ON `ratings` (`session`);
CREATE TABLE IF NOT EXISTS `reports` (
	`id` text PRIMARY KEY NOT NULL,
	`session` text NOT NULL,
	`reporter` text NOT NULL,
	`reason` text NOT NULL,
	`status` text DEFAULT 'open' NOT NULL,
	`created` integer NOT NULL
);

CREATE TABLE IF NOT EXISTS `sessions` (
	`id` text PRIMARY KEY NOT NULL,
	`speaker` text NOT NULL,
	`listener` text,
	`mode` text NOT NULL,
	`status` text DEFAULT 'waiting' NOT NULL,
	`created` integer NOT NULL,
	`ended` integer
);

CREATE TABLE IF NOT EXISTS `users` (
	`id` text PRIMARY KEY NOT NULL,
	`name` text NOT NULL,
	`role` text DEFAULT 'speaker' NOT NULL,
	`status` text DEFAULT 'none' NOT NULL,
	`bio` text DEFAULT '' NOT NULL,
	`languages` text DEFAULT 'English' NOT NULL,
	`modes` text DEFAULT 'chat' NOT NULL,
	`available` integer DEFAULT 0 NOT NULL,
	`seen` integer DEFAULT 0 NOT NULL,
	`consent` integer DEFAULT 0 NOT NULL
);

CREATE INDEX IF NOT EXISTS participant_speaker ON sessions(speaker,status);
CREATE INDEX IF NOT EXISTS participant_listener ON sessions(listener,status);
CREATE INDEX IF NOT EXISTS session_messages ON messages(session,created);
