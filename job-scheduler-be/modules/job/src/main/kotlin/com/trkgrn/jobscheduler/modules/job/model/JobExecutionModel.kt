package com.trkgrn.jobscheduler.modules.job.model

import com.trkgrn.jobscheduler.platform.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(schema = "public", name = "job_executions")
class JobExecutionModel @JvmOverloads constructor(
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "job_execution", sequenceName = "job_execution_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "job_execution", strategy = GenerationType.SEQUENCE)
    override var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = CronJobModel::class)
    @JoinColumn(name = "job_definition_id", referencedColumnName = "id")
    var jobDefinition: CronJobModel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Status = Status.QUEUED,

    @Column(name = "started_at")
    var startedAt: OffsetDateTime? = null,

    @Column(name = "ended_at")
    var endedAt: OffsetDateTime? = null,

    @Column(name = "attempt", nullable = false)
    var attempt: Int = 1,

    @Column(name = "correlation_id")
    var correlationId: String? = null,

    @Column(name = "node_id")
    var nodeId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    var parameters: Map<String, Any>? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "logs", columnDefinition = "jsonb")
    var logs: List<LogEntry>? = null,

    @Column(name = "log_level")
    var logLevel: String? = null
) : BaseEntity() {

    enum class Status { QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED }
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String
    )
}

