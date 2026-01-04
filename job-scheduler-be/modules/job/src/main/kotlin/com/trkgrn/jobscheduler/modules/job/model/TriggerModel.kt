package com.trkgrn.jobscheduler.modules.job.model

import com.trkgrn.jobscheduler.platform.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

/**
 * Trigger model for scheduling CronJobs
 */
@Entity
@Table(schema = "public", name = "triggers")
class TriggerModel @JvmOverloads constructor(
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "trigger", sequenceName = "trigger_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "trigger", strategy = GenerationType.SEQUENCE)
    override var id: Long? = null,

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = CronJobModel::class)
    @JoinColumn(name = "cron_job_id", referencedColumnName = "id")
    var cronJob: CronJobModel? = null,

    @Column(name = "cron_expression", nullable = false)
    var cronExpression: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "start_time")
    var startTime: OffsetDateTime? = null,

    @Column(name = "end_time")
    var endTime: OffsetDateTime? = null,

    @Column(name = "timezone", nullable = false)
    var timezone: String = "UTC",

    @Column(name = "priority", nullable = false)
    var priority: Int = 5,

    @Column(name = "misfire_instruction")
    var misfireInstruction: String? = "SMART_POLICY",

    @Column(name = "quartz_trigger_key")
    var quartzTriggerKey: String? = null,

    @Column(name = "last_fire_time")
    var lastFireTime: OffsetDateTime? = null,

    @Column(name = "next_fire_time")
    var nextFireTime: OffsetDateTime? = null,

    @Column(name = "fire_count", nullable = false)
    var fireCount: Long = 0,

    @Column(name = "max_fire_count")
    var maxFireCount: Long? = null
) : BaseEntity() {

    enum class MisfireInstruction {
        SMART_POLICY,
        IGNORE_MISFIRE_POLICY,
        FIRE_ONCE_NOW,
        DO_NOTHING
    }
}

