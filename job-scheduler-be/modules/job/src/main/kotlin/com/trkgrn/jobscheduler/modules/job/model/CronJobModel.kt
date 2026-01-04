package com.trkgrn.jobscheduler.modules.job.model

import com.trkgrn.jobscheduler.platform.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

/**
 * Base CronJob model
 * All specific job models should extend this
 */
@Entity
@Table(schema = "public", name = "cron_jobs")
@Inheritance(strategy = InheritanceType.JOINED)
open class CronJobModel @JvmOverloads constructor(
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "cron_job", sequenceName = "cron_job_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "cron_job", strategy = GenerationType.SEQUENCE)
    override var id: Long? = null,

    @Column(name = "code", nullable = false, unique = true)
    open var code: String? = null,

    @Column(name = "name", nullable = false)
    open var name: String? = null,

    @Column(name = "description")
    open var description: String? = null,

    @Column(name = "enabled", nullable = false)
    open var enabled: Boolean = true,

    @Column(name = "job_bean_name", nullable = false)
    open var jobBeanName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    open var status: CronJobStatus = CronJobStatus.UNKNOWN,

    @Column(name = "last_start_time")
    open var lastStartTime: OffsetDateTime? = null,

    @Column(name = "last_end_time")
    open var lastEndTime: OffsetDateTime? = null,

    @Column(name = "last_result")
    open var lastResult: String? = null,

    @Column(name = "retry_count", nullable = false)
    open var retryCount: Int = 0,

    @Column(name = "max_retry_count", nullable = false)
    open var maxRetryCount: Int = 3,

    @Column(name = "node_id")
    open var nodeId: String? = null,

    @Column(name = "correlation_id")
    open var correlationId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    open var parameters: Map<String, Any>? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false)
    open var logLevel: LogLevel = LogLevel.INFO,

    @OneToMany(mappedBy = "jobDefinition", cascade = [CascadeType.ALL], orphanRemoval = true)
    open var executions: MutableList<JobExecutionModel> = mutableListOf()
) : BaseEntity() {

    enum class LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, OFF
    }
}

