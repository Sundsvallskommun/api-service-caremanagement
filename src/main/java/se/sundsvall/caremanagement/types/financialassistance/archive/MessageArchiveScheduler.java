package se.sundsvall.caremanagement.types.financialassistance.archive;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Nightly job that archives the conversation of every long-closed financial assistance errand into a Lifecare-bound
 * message history
 * PDF. Thin trigger — all work (and per-errand error isolation) lives in {@link MessageArchiveService}. ShedLock keeps
 * a single node running it at a time.
 */
@Component
@EnableConfigurationProperties(MessageArchiveProperties.class)
class MessageArchiveScheduler {

	private final MessageArchiveService messageArchiveService;

	MessageArchiveScheduler(final MessageArchiveService messageArchiveService) {
		this.messageArchiveService = messageArchiveService;
	}

	@Dept44Scheduled(cron = "${scheduler.message-archive.cron}",
		name = "${scheduler.message-archive.name}",
		lockAtMostFor = "${scheduler.message-archive.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.message-archive.maximum-execution-time}")
	void archiveClosedErrandConversations() {
		messageArchiveService.archiveClosedErrands();
	}
}
