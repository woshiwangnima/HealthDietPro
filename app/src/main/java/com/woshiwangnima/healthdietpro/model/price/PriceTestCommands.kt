package com.woshiwangnima.healthdietpro.model.price

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * 【测试指令 · 记物价】的指令集。
 *
 * 与记便签一样定义在模型层：生成器与执行器都吃它，单测可以直接构造指令跑纯函数。
 * 参数都指向**能被界面看见的形态**——合并区间卡、跨地区、跨月份、单位分行、走势折线，
 * 而不是「随便写几条数字」。
 */
internal sealed interface PriceTestCommand {

    /**
     * 创建随机物价记录。
     *
     * @param count 记录条数
     * @param itemCount 用多少个不同物品（同一物品的多条记录才会在统计里合并成区间）
     * @param regionCount 用多少个地区（0 表示不填地区，测「无地区」那一档）
     * @param sellUnits 售卖单位，逗号分隔（如 `斤,公斤,个`）——不同单位在统计里分行，不互相比较
     * @param priceUnit 物价单位（元 / 角 / 美元）
     * @param recentDays 记录日期落在最近几天内（跨月、跨年由它决定）
     * @param rangePercent 填了价格上限的记录占比（%），造「3.2–4.5」这种区间卡
     * @param peersPerKey 同一天同地区同物品同单位最多重复几条——多条会被合并成一条区间卡
     * @param notePercent 带备注的记录占比（%）
     * @param attachToLibrary 挂到库内食材/食物/菜肴（库里没有条目时退化为自定义名）
     */
    data class CreateRecords(
        val count: Int,
        val itemCount: Int,
        val regionCount: Int,
        val sellUnits: String,
        val priceUnit: String,
        val recentDays: Int,
        val rangePercent: Int,
        val peersPerKey: Int,
        val notePercent: Int,
        val attachToLibrary: Boolean,
    ) : PriceTestCommand

    /**
     * 造一条连续走势：同一物品、同一售卖单位、逐日一条，价格按给定波动率随机游走。
     * 专门喂【物品历史】的折线图与它上面的点按浮窗。
     */
    data class CreateTrend(
        val itemName: String,
        val days: Int,
        val sellUnit: String,
        val priceUnit: String,
        val basePrice: String,
        val fluctuationPercent: Int,
        val regionCount: Int,
    ) : PriceTestCommand

    /** 清空全部物价记录。 */
    data object DeleteAllRecords : PriceTestCommand
}

/** 一次指令执行的结果计数，由调用方拼 Toast 文案。 */
internal data class PriceTestResult(
    val createdRecords: Int = 0,
    val deletedRecords: Int = 0,
)

/** 库内物品的轻量引用：测试数据只需要「id + 名字 + 来源」三样，不把 FoodItem 拖进生成器。 */
internal data class PriceFoodRef(
    val id: String,
    val name: String,
    val source: PriceItemSource,
)

/** 随机物价记录生成参数（UI 输入的收敛结果）。 */
internal data class RandomPriceSpec(
    val count: Int,
    val itemCount: Int = 5,
    val regionCount: Int = 2,
    val sellUnits: List<String> = listOf("斤"),
    val priceUnit: String = "元",
    val recentDays: Int = 30,
    val rangePercent: Int = 40,
    val peersPerKey: Int = 1,
    val notePercent: Int = 20,
    val attachToLibrary: Boolean = false,
)

/** 走势生成参数。 */
internal data class PriceTrendSpec(
    val itemName: String,
    val days: Int = 30,
    val sellUnit: String = "斤",
    val priceUnit: String = "元",
    val basePrice: Double = 5.0,
    val fluctuationPercent: Int = 12,
    val regionCount: Int = 1,
)

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val MAX_TEST_PRICE_RECORDS = 1_000
private const val MAX_TEST_PRICE_ITEMS = 60
private const val MAX_TEST_PRICE_REGIONS = 6
private const val MAX_TEST_PRICE_DAYS = 365
private const val MAX_TEST_PRICE_PEERS = 5
private const val MAX_TEST_TREND_DAYS = 400

/** 手输物品名池：菜场里最常被记价的那几样。 */
private val ITEM_POOL = listOf(
    "土豆", "西红柿", "黄瓜", "鸡蛋", "白菜", "豆角", "青椒",
    "苹果", "香蕉", "牛奶", "大米", "猪肉后腿", "鸡胸肉", "豆腐",
)

/**
 * 地区池：省 / 市 / 区三级都带 GB/T 2260 代码与质心坐标，
 * 生成的数据与「按定位选地区」产生的记录长得一样，筛选与地区展示才有得测。
 */
internal data class PriceRegionSeed(
    val provinceCode: String,
    val provinceName: String,
    val cityCode: String,
    val cityName: String,
    val districtCode: String,
    val districtName: String,
    val lng: Double,
    val lat: Double,
)

internal val PRICE_REGION_POOL = listOf(
    PriceRegionSeed("11", "北京市", "1101", "北京市", "110105", "朝阳区", 116.486, 39.921),
    PriceRegionSeed("31", "上海市", "3101", "上海市", "310115", "浦东新区", 121.544, 31.221),
    PriceRegionSeed("44", "广东省", "4401", "广州市", "440106", "天河区", 113.361, 23.124),
    PriceRegionSeed("51", "四川省", "5101", "成都市", "510107", "武侯区", 104.043, 30.642),
    PriceRegionSeed("33", "浙江省", "3301", "杭州市", "330106", "西湖区", 120.130, 30.259),
    PriceRegionSeed("42", "湖北省", "4201", "武汉市", "420111", "洪山区", 114.343, 30.499),
)

/** 把「斤, 公斤 个」这类手输拆成单位列表；空输入退回单个默认单位。 */
internal fun parseSellUnits(raw: String): List<String> =
    raw.split(',', '，', '、', ' ')
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .ifEmpty { listOf("斤") }
        .take(4)

/**
 * 随机物价记录。
 *
 * 生成按「身份键组」推进而不是逐条随机：先抽一个 (物品, 地区, 日期, 单位)，
 * 再给它配上 1..[RandomPriceSpec.peersPerKey] 条记录，同组价格各不相同——
 * 这样列表页才会出现「一条卡里 3.2–4.5」的合并区间，而不是一堆永远不合并的单值卡。
 */
internal fun buildRandomPriceRecords(
    spec: RandomPriceSpec,
    libraryItems: List<PriceFoodRef>,
    now: Long,
    zone: ZoneId,
    random: Random,
): List<PriceRecord> {
    val count = spec.count.coerceIn(0, MAX_TEST_PRICE_RECORDS)
    if (count == 0) return emptyList()

    val items = pickItems(spec, libraryItems, random)
    val regions = PRICE_REGION_POOL.take(spec.regionCount.coerceIn(0, MAX_TEST_PRICE_REGIONS))
    val units = spec.sellUnits.map(String::trim).filter(String::isNotBlank).ifEmpty { listOf("斤") }
    val priceUnit = spec.priceUnit.trim().ifBlank { "元" }
    val recentDays = spec.recentDays.coerceIn(1, MAX_TEST_PRICE_DAYS)
    val rangePercent = spec.rangePercent.coerceIn(0, 100)
    val peers = spec.peersPerKey.coerceIn(1, MAX_TEST_PRICE_PEERS)
    val notePercent = spec.notePercent.coerceIn(0, 100)

    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val records = mutableListOf<PriceRecord>()
    var index = 0
    while (records.size < count) {
        val item = items.random(random)
        val region = if (regions.isEmpty()) null else regions.random(random)
        val unit = units.random(random)
        val day = today.minusDays(random.nextLong(recentDays.toLong()))
        val recordedAt = day.atStartOfDay(zone).toInstant().toEpochMilli()
        // 同组共享的物品/地区/日期/单位一致，只有价格不同——这才是「合并成区间」的前提。
        val priceBase = randomPrice(random)
        val groupSize = random.nextInt(1, peers + 1).coerceAtMost(count - records.size)
        repeat(groupSize) { peerIndex ->
            val low = round2(priceBase * (1.0 + random.nextInt(-8, 9) / 100.0)).coerceAtLeast(0.01)
            val high = if (random.nextInt(100) < rangePercent) {
                round2(low * (1.0 + random.nextInt(5, 41) / 100.0)).coerceAtLeast(low)
            } else {
                null
            }
            val createdAt = recordedAt - random.nextLong(1, 30) * DAY_MS
            val updatedAt = (recordedAt + random.nextLong(0, 8 * 60 * 60 * 1000L))
                .coerceAtMost(now)
                .coerceAtLeast(createdAt + 1_000L)
            records += PriceRecord(
                id = "test_price_${now}_${index}_$peerIndex",
                itemSource = item.source,
                itemId = item.itemId,
                itemName = item.name,
                price = low,
                priceMax = high,
                priceUnit = priceUnit,
                sellUnit = unit,
                provinceCode = region?.provinceCode.orEmpty(),
                provinceName = region?.provinceName.orEmpty(),
                cityCode = region?.cityCode.orEmpty(),
                cityName = region?.cityName.orEmpty(),
                districtCode = region?.districtCode.orEmpty(),
                districtName = region?.districtName.orEmpty(),
                lng = region?.lng ?: 0.0,
                lat = region?.lat ?: 0.0,
                recordedAt = recordedAt,
                note = if (random.nextInt(100) < notePercent) "测试备注 ${random.nextInt(100, 999)}" else "",
                createdAtMillis = createdAt,
                updatedAtMillis = updatedAt,
            )
            index++
        }
    }
    return records
}

/**
 * 连续 N 天的走势：同一天可能多地区各一条（图上收成一个区间点），
 * 价格按 ±波动率随机游走，跌到 0.01 以下就贴住地板。
 */
internal fun buildPriceTrend(
    spec: PriceTrendSpec,
    now: Long,
    zone: ZoneId,
    random: Random,
): List<PriceRecord> {
    val days = spec.days.coerceIn(2, MAX_TEST_TREND_DAYS)
    val itemName = spec.itemName.trim().ifBlank { "测试物品" }
    val sellUnit = spec.sellUnit.trim().ifBlank { "斤" }
    val priceUnit = spec.priceUnit.trim().ifBlank { "元" }
    val fluctuation = spec.fluctuationPercent.coerceIn(0, 60) / 100.0
    val regions = PRICE_REGION_POOL.take(spec.regionCount.coerceIn(0, MAX_TEST_PRICE_REGIONS))
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    var price = spec.basePrice.takeIf { it > 0.0 } ?: 5.0
    return (0 until days).flatMap { dayIndex ->
        val day = today.minusDays((days - 1 - dayIndex).toLong())
        val recordedAt = day.atStartOfDay(zone).toInstant().toEpochMilli()
        // 每天先走一步：趋势是「昨天的价格再动一点」，不是每天独立随机。
        // 波动率为 0 时不动（`nextDouble(0.0, 0.0)` 是非法的空区间）。
        price = if (fluctuation > 0.0) {
            price * (1.0 + random.nextDouble(-fluctuation, fluctuation))
        } else {
            price
        }
        price = price.coerceAtLeast(0.01)
        val targets = if (regions.isEmpty()) listOf(null) else regions
        // 同一天多地区才需要「地区间差价」：只有一个来源时价格就是当天基准，
        // 否则波动率填 0 也会每天飘一点，折线图与「平价」这种设定就对不上了。
        val multiRegion = targets.size > 1
        targets.mapIndexed { regionIndex, region ->
            val low = if (multiRegion) {
                round2(price * (1.0 + random.nextInt(-6, 7) / 100.0)).coerceAtLeast(0.01)
            } else {
                round2(price).coerceAtLeast(0.01)
            }
            PriceRecord(
                id = "test_trend_${now}_${dayIndex}_$regionIndex",
                itemSource = PriceItemSource.CUSTOM,
                itemId = "",
                itemName = itemName,
                price = low,
                priceMax = null,
                priceUnit = priceUnit,
                sellUnit = sellUnit,
                provinceCode = region?.provinceCode.orEmpty(),
                provinceName = region?.provinceName.orEmpty(),
                cityCode = region?.cityCode.orEmpty(),
                cityName = region?.cityName.orEmpty(),
                districtCode = region?.districtCode.orEmpty(),
                districtName = region?.districtName.orEmpty(),
                lng = region?.lng ?: 0.0,
                lat = region?.lat ?: 0.0,
                recordedAt = recordedAt,
                note = "",
                createdAtMillis = recordedAt + 8 * 60 * 60 * 1000L,
                updatedAtMillis = recordedAt + 9 * 60 * 60 * 1000L,
            )
        }
    }
}

/** 物品池：勾了「挂到库内物品」就优先用真实食材/食物/菜肴，库里没有条目时退回手输名。 */
private fun pickItems(
    spec: RandomPriceSpec,
    libraryItems: List<PriceFoodRef>,
    random: Random,
): List<PickedItem> {
    val wanted = spec.itemCount.coerceIn(1, MAX_TEST_PRICE_ITEMS)
    if (spec.attachToLibrary && libraryItems.isNotEmpty()) {
        return libraryItems.shuffled(random).take(wanted).map {
            PickedItem(name = it.name, itemId = it.id, source = it.source)
        }
    }
    // 物品种类多于词池时用序号补后缀：名字不同才算不同物品，统计里才能区分。
    return (0 until wanted).map { index ->
        val base = ITEM_POOL[index % ITEM_POOL.size]
        val name = if (index < ITEM_POOL.size) base else "$base${index / ITEM_POOL.size + 1}"
        PickedItem(name = name, itemId = "", source = PriceItemSource.CUSTOM)
    }
}

private data class PickedItem(
    val name: String,
    val itemId: String,
    val source: PriceItemSource,
)

private fun randomPrice(random: Random): Double = round2(1.0 + random.nextDouble(0.0, 59.0))

private fun round2(value: Double): Double =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

/**
 * 指令执行器：一次 `store.update` 落一条指令。
 * [library] 只在「挂到库内物品」时才被调用，加载几千条物品的代价不该平摊到每条指令上。
 */
internal class PriceTestCommandRunner(
    private val store: PriceArchiveStore,
    private val library: (() -> List<PriceFoodRef>)? = null,
) {
    fun run(command: PriceTestCommand): PriceTestResult = when (command) {
        is PriceTestCommand.CreateRecords -> createRecords(command)

        is PriceTestCommand.CreateTrend -> createTrend(command)

        PriceTestCommand.DeleteAllRecords -> deleteAll()
    }

    private fun createRecords(command: PriceTestCommand.CreateRecords): PriceTestResult {
        val libraryItems = if (command.attachToLibrary) {
            runCatching { library?.invoke().orEmpty() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(
                count = command.count,
                itemCount = command.itemCount,
                regionCount = command.regionCount,
                sellUnits = parseSellUnits(command.sellUnits),
                priceUnit = command.priceUnit,
                recentDays = command.recentDays,
                rangePercent = command.rangePercent,
                peersPerKey = command.peersPerKey,
                notePercent = command.notePercent,
                attachToLibrary = command.attachToLibrary,
            ),
            libraryItems = libraryItems,
            now = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            random = Random(System.nanoTime()),
        )
        store.update { archive ->
            archive.copy(records = (archive.records + records).sortedByDescending(PriceRecord::recordedAt))
        }
        return PriceTestResult(createdRecords = records.size)
    }

    private fun createTrend(command: PriceTestCommand.CreateTrend): PriceTestResult {
        val records = buildPriceTrend(
            spec = PriceTrendSpec(
                itemName = command.itemName,
                days = command.days,
                sellUnit = command.sellUnit,
                priceUnit = command.priceUnit,
                basePrice = command.basePrice.trim().toDoubleOrNull() ?: 5.0,
                fluctuationPercent = command.fluctuationPercent,
                regionCount = command.regionCount,
            ),
            now = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            random = Random(System.nanoTime()),
        )
        store.update { archive ->
            archive.copy(records = (archive.records + records).sortedByDescending(PriceRecord::recordedAt))
        }
        return PriceTestResult(createdRecords = records.size)
    }

    private fun deleteAll(): PriceTestResult {
        var deleted = 0
        store.update { archive ->
            deleted = archive.records.size
            archive.copy(records = emptyList())
        }
        return PriceTestResult(deletedRecords = deleted)
    }
}
