package com.example.frontiercommoditytrader

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import android.view.SoundEffectConstants
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.frontiercommoditytrader.ui.theme.TheDopestDealsTheme
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TheDopestDealsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TheDopestDealsApp()
                }
            }
        }
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    M3Button(
        onClick = {
            val prefs = context.getSharedPreferences("dopest_deals", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("is_muted", false)) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    M3OutlinedButton(
        onClick = {
            val prefs = context.getSharedPreferences("dopest_deals", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("is_muted", false)) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

private fun chooseTravelSoundRes(day: Int, cityName: String): Int = R.raw.carstop

@Composable
private fun TravelSoundEffect(day: Int, cityName: String, enabled: Boolean, isMuted: Boolean, hasEncounter: Boolean) {
    val context = LocalContext.current
    val isFirstRender = remember { mutableStateOf(true) }
    val lastDay = remember { mutableStateOf(day) }
    val lastCity = remember { mutableStateOf(cityName) }
    LaunchedEffect(day, cityName) {
        if (isFirstRender.value) {
            isFirstRender.value = false
            lastDay.value = day
            lastCity.value = cityName
            return@LaunchedEffect
        }
        if (!enabled || isMuted || hasEncounter) return@LaunchedEffect
        if (day != lastDay.value || cityName != lastCity.value) {
            lastDay.value = day
            lastCity.value = cityName
            val player = MediaPlayer.create(context, chooseTravelSoundRes(day, cityName))
            player?.setOnCompletionListener { mp -> mp.release() }
            player?.start()
        }
    }
}

enum class GameLength(val days: Int) { DAYS_30(30), DAYS_60(60), DAYS_90(90), DAYS_120(120) }
enum class VendorType { WEAPONS, ARMOR, COMMODITIES }
enum class EncounterType { THIEVES, COPS, FREE_STASH, PLUMMET, SPEEDING, VEHICLE_SEARCH }
enum class CombatTarget { PLAYER, ENEMY }

data class CommodityDef(
    val name: String,
    val minPrice: Int,
    val maxPrice: Int,
    val volatility: Int,
    val blurb: String,
)

data class WeaponDef(
    val name: String,
    val minPrice: Int,
    val maxPrice: Int,
    val damage: Int,
    val ammoName: String?,
    val ammoMinPrice: Int,
    val ammoMaxPrice: Int,
    val ammoPerShot: Int,
)

data class ArmorDef(
    val name: String,
    val price: Int,
    val defense: Int,
)

data class MarketCommodity(val def: CommodityDef, val price: Int, var qty: Int)
data class MarketWeapon(val def: WeaponDef, val price: Int, var qty: Int)
data class MarketAmmo(val weaponName: String, val ammoName: String, val price: Int, var qty: Int)
data class MarketArmor(val def: ArmorDef, var qty: Int)

data class InventoryItem(
    val name: String,
    val type: VendorType,
    val quantity: Int,
    val avgCost: Int,
)

data class CityDef(
    val name: String,
    val summary: String,
    val richBias: Int = 0,
    val cheapCommodity: String? = null,
    val cheapBias: Int = 0,
)

data class CityVisit(
    val city: CityDef,
    val daySeed: Int,
    val contraband: String,
    val commodities: List<MarketCommodity>,
    val marketEventText: String = "Street prices feel normal today.",
    val plummetTarget: String? = null,
    val weaponsAvailable: Boolean,
    val weaponStock: List<MarketWeapon>,
    val ammoStock: List<MarketAmmo>,
    val armorAvailable: Boolean,
    val armorStock: List<MarketArmor>,
)

data class ScoreEntry(
    val playerName: String,
    val days: Int,
    val finalNetWorth: Int,
    val city: String,
    val rank: String,
)

data class Loan(val amount: Int, val dueDay: Int)

data class MobsterState(
    val name: String = "Vinnie \"The Knuckle\" Moretti",
    val debtDueDay: Int = 11,
    val lastPenaltyStage: Int = 0,
    val dailyBorrowed: Int = 0,
    val latestThreat: String = "Vinnie is waiting for his money.",
    val loans: List<Loan> = emptyList()
)

data class EncounterState(
    val type: EncounterType,
    val title: String,
    val text: String,
    val cityContraband: String,
    val enemyHealth: Int = 100,
    val playerTurnText: String = "",
)

data class GameState(
    val started: Boolean = false,
    val gameOver: Boolean = false,
    val playerName: String = "Dealer",
    val selectedLength: GameLength = GameLength.DAYS_30,
    val day: Int = 1,
    val cash: Int = 2000,
    val debt: Int = 2000,
    val bankSavings: Int = 0,
    val health: Int = 100,
    val stashCapacity: Int = 30,
    val armorDefense: Int = 0,
    val inventory: List<InventoryItem> = emptyList(),
    val currentVisit: CityVisit = generateCityVisit(cities.first(), 1),
    val message: String = "Start a run.",
    val eventLog: String = "No travel event yet.",
    val mobster: MobsterState = initialMobsterLoan(1),
    val activeEncounter: EncounterState? = null,
    val travelChoices: List<String> = emptyList(),
    val leaderboard: List<ScoreEntry> = emptyList(),
    val vinniePenaltyAmount: Int = 0,
) {
    fun usedStash(): Int = inventory.filter { it.type == VendorType.COMMODITIES }.sumOf { it.quantity }
    fun freeStash(): Int = stashCapacity - usedStash()
    fun ammoFor(weapon: WeaponDef): Int = inventory.firstOrNull { it.name == (weapon.ammoName ?: "") }?.quantity ?: 0
    fun weaponOwned(weapon: WeaponDef): Int = inventory.firstOrNull { it.name == weapon.name }?.quantity ?: 0
    fun inventoryValue(): Int {
        val commodityPrices = currentVisit.commodities.associate { it.def.name to it.price }
        return inventory.sumOf { item ->
            when (item.type) {
                VendorType.COMMODITIES -> (commodityPrices[item.name] ?: item.avgCost) * item.quantity
                VendorType.WEAPONS, VendorType.ARMOR -> item.avgCost * item.quantity
            }
        }
    }
    fun netWorth(): Int = cash + bankSavings + inventoryValue() - debt
    fun rankName(): String = when {
        netWorth() >= 100000 -> "Deal Legend"
        netWorth() >= 70000 -> "Cartel Kingpin"
        netWorth() >= 40000 -> "Street Shark"
        netWorth() >= 20000 -> "Corner Hustler"
        else -> "Rookie Runner"
    }
    fun overdueDays(): Int = max(0, day - mobster.debtDueDay)
    fun contrabandQuantity(): Int = inventory.firstOrNull { it.name == currentVisit.contraband }?.quantity ?: 0
}

private const val LOAN_TERM_DAYS = 10
private const val THIEF_DAMAGE = 18
private const val COP_DAMAGE = 20

private val cities = listOf(
    CityDef("Rusthaven", "Scrapyard alleys and hard-nosed haggling.", cheapCommodity = "Shake", cheapBias = -20, richBias = 5),
    CityDef("Blackport", "Dockside hustlers move stash under dim lights.", cheapCommodity = "Opium", cheapBias = -16, richBias = 8),
    CityDef("Iron Mesa", "Mine money and ore caravans keep the streets loud.", cheapCommodity = "MDMA", cheapBias = -28, richBias = 10),
    CityDef("Dustveil", "Sandstorms hide deals and theft in equal measure.", cheapCommodity = "Special K", cheapBias = -14, richBias = 4),
    CityDef("Northreach", "Cold roads, rich buyers, and expensive shortages.", richBias = 18),
    CityDef("Cinder Bay", "Fuel, heat, and machine noise everywhere.", cheapCommodity = "Klonopins", cheapBias = -26, richBias = 12),
    CityDef("Stormwatch", "A walled market with armored guards and fat purses.", richBias = 16),
    CityDef("Red Quarry", "Stone dust and ore fortunes.", cheapCommodity = "MDMA", cheapBias = -18, richBias = 9),
    CityDef("Ashfall", "Danger routes make medkits and fuel jump in price.", richBias = 15),
    CityDef("Goldmere", "Big money town where markups get ridiculous.", richBias = 22),
)

private val commodities = listOf(
    CommodityDef("Shake", 80, 180, 20, "Bottom-shelf street bundles flipped for quick cash."),
    CommodityDef("Opium", 120, 240, 22, "Sticky black bricks sold cheap in rough corners."),
    CommodityDef("Dabs", 160, 300, 24, "Little tins of glossy concentrate with steady buzz."),
    CommodityDef("Hydro Tabs", 220, 420, 28, "Pale powder wraps that move quietly through alley deals."),
    CommodityDef("Klonopins", 320, 560, 32, "Glittery blue chips with heat-sealed street packaging."),
    CommodityDef("MDMA", 420, 700, 34, "Dark party powder that surges after sundown."),
    CommodityDef("Mescaline", 560, 900, 38, "Premium haze packs with jumpy price swings."),
    CommodityDef("Special K", 700, 1100, 42, "Stamped paper hits with boutique undercity hype."),
    CommodityDef("Crack", 900, 1400, 44, "Chunky street stones with ugly volatility."),
    CommodityDef("Morphine", 1100, 1700, 46, "Pressed drops that disappear fast in hard towns."),
    CommodityDef("Oxys", 1300, 1900, 48, "Premium marked strips favored by rich buyers."),
    CommodityDef("Meth", 1500, 2200, 50, "Sealed ampules whispered about in clinic back rooms."),
    CommodityDef("Coke", 1800, 2600, 54, "Potent red synth packs that can spike overnight."),
    CommodityDef("Whippets", 2100, 3000, 56, "Bright festival tabs that travel well."),
    CommodityDef("Shrooms", 2400, 3400, 58, "Rare caps hauled in from back-road growers."),
    CommodityDef("Peyote", 2800, 3900, 60, "Clean dissociation powder cut for velvet-room buyers."),
    CommodityDef("Xanies", 3200, 4500, 62, "Designer party presses stamped with flashy logos."),
    CommodityDef("Fentanyl", 3600, 5000, 64, "Rare smoke packs whispered about in alley markets."),
    CommodityDef("Heroin", 4200, 5800, 68, "Mystic buttons hauled from long-haul caravans."),
    CommodityDef("LSD", 5000, 7000, 72, "Heavy steel canisters that sell best in wealthy zones."),
)

private val weaponDefs = listOf(
    WeaponDef("Boot Knife", 1000, 1250, 10, null, 0, 0, 0),
    WeaponDef("22 Caliber Pistol", 3000, 3750, 15, ".22 Ammo", 100, 160, 1),
    WeaponDef(".380 Pistol", 9000, 11000, 25, ".380 Ammo", 180, 260, 1),
    WeaponDef("Katana", 15000, 15000, 30, null, 0, 0, 0),
    WeaponDef("12 Gauge Shotgun", 22000, 24000, 40, "12 Gauge Shell", 320, 420, 1),
    WeaponDef("AK47", 30000, 35000, 50, "7.62 Ammo", 500, 650, 1),
    WeaponDef("Rocket Launcher", 50000, 75000, 80, "Rocket", 900, 1000, 1),
)

private val armorDefs = listOf(
    ArmorDef("Fanny pack", 5000, 0), // Special item to prevent theft
    ArmorDef("Leather Jacket", 1400, 5),
    ArmorDef("Kevlar Vest", 4200, 10),
    ArmorDef("Ballistic Armor", 9000, 18),
)

private fun seededRandom(vararg keys: Any): Random {
    var seed = 29L
    keys.forEach { seed = seed * 37 + it.hashCode() }
    return Random(seed)
}

private fun generateCityVisit(city: CityDef, day: Int): CityVisit {
    val rng = seededRandom(city.name, day)
    val contrabandDef = commodities.shuffled(rng).first()
    val contraband = contrabandDef.name
    
    val otherCommodities = commodities.filter { it.name != contraband }.shuffled(rng).take(rng.nextInt(4, 8))
    val listed = (otherCommodities + contrabandDef).shuffled(rng)

    var marketEventText = "Street prices feel normal today."
    var spikeTarget: String? = null
    var glutTarget: String? = null
    var plummetTarget: String? = null
    
    val eventChance = rng.nextInt(100)
    when {
        eventChance < 15 -> { // 15% chance for a massive plummet event
            plummetTarget = listed.random(rng).name
        }
        eventChance in 15..24 -> { // 10% chance
            spikeTarget = listed.random(rng).name
            marketEventText = "Shortage rumor: $spikeTarget is drying up, and street sellers are gouging buyers."
        }
        eventChance in 25..34 -> { // 10% chance
            glutTarget = listed.random(rng).name
            marketEventText = "Oversupply rumor: too much $glutTarget hit the alleys, so prices are crashing."
        }
    }
    
    val commodityStock = listed.map { def ->
        val base = rng.nextInt(def.minPrice, def.maxPrice + 1)
        val bias = if (city.cheapCommodity == def.name) city.cheapBias else 0
        var price = max(20, base + bias + city.richBias + rng.nextInt(-def.volatility, def.volatility + 1))
        
        if (def.name == spikeTarget) price = max(price + max(100, (price * 0.9).toInt()), (price * 18) / 10)
        if (def.name == glutTarget) price = max(20, min((price * 45) / 100, price - max(80, (price * 35) / 100)))
        if (def.name == plummetTarget) {
            val dropPct = rng.nextInt(20, 51) / 100f // 20% to 50% multiplier
            price = max(5, (price * dropPct).toInt())
        }
        if (def.name == contraband) {
            price *= rng.nextInt(10, 21) // 10x to 20x normal value for contraband
        }
        MarketCommodity(def, price, rng.nextInt(1, 31))
    }.sortedBy { it.price }

    val weaponsAvailable = rng.nextInt(100) < 35
    val weaponStock = if (weaponsAvailable) {
        weaponDefs.shuffled(rng).take(3).map { def -> MarketWeapon(def, rng.nextInt(def.minPrice, def.maxPrice + 1), 1) }
    } else emptyList()
    val ammoStock = weaponStock.mapNotNull { stock ->
        val ammoName = stock.def.ammoName ?: return@mapNotNull null
        MarketAmmo(stock.def.name, ammoName, rng.nextInt(stock.def.ammoMinPrice, stock.def.ammoMaxPrice + 1), 1)
    }

    val armorAvailable = rng.nextInt(100) < 35
    val armorStock = if (armorAvailable) {
        val stock = armorDefs.filter { it.name != "Fanny pack" }.shuffled(rng).take(2).toMutableList()
        if (rng.nextInt(100) < 40) {
            armorDefs.find { it.name == "Fanny pack" }?.let { stock.add(it) }
        }
        stock.map { MarketArmor(it, 1) }
    } else emptyList()

    return CityVisit(
        city = city,
        daySeed = day,
        contraband = contraband,
        commodities = commodityStock,
        marketEventText = marketEventText,
        plummetTarget = plummetTarget,
        weaponsAvailable = weaponsAvailable,
        weaponStock = weaponStock,
        ammoStock = ammoStock,
        armorAvailable = armorAvailable,
        armorStock = armorStock,
    )
}

private fun initialMobsterLoan(day: Int): MobsterState {
    val due = day + LOAN_TERM_DAYS - 1
    return MobsterState(
        debtDueDay = due,
        latestThreat = "Vinnie is tracking your debt. Payment is due by day $due.",
        loans = listOf(Loan(2000, due))
    )
}

private fun addItem(inventory: List<InventoryItem>, name: String, type: VendorType, qty: Int, unitCost: Int): List<InventoryItem> {
    val current = inventory.firstOrNull { it.name == name }
    return if (current == null) {
        inventory + InventoryItem(name, type, qty, unitCost)
    } else {
        inventory.map {
            if (it.name == name) {
                val totalCost = (it.avgCost * it.quantity) + (unitCost * qty)
                val newQty = it.quantity + qty
                it.copy(quantity = newQty, avgCost = totalCost / newQty)
            } else it
        }
    }
}

private fun removeItems(inventory: List<InventoryItem>, name: String, qty: Int): List<InventoryItem> =
    inventory.mapNotNull {
        if (it.name == name) {
            val remaining = it.quantity - qty
            if (remaining > 0) it.copy(quantity = remaining) else null
        } else it
    }

private fun removeOne(inventory: List<InventoryItem>, name: String): List<InventoryItem> =
    removeItems(inventory, name, 1)

private fun buyCommodity(state: GameState, item: MarketCommodity, qty: Int): GameState {
    val maxBuy = min(qty, item.qty)
    if (maxBuy <= 0) return state
    val cost = item.price * maxBuy
    if (state.cash < cost) return state.copy(message = "you dont have enough cash dude!")
    if (state.freeStash() < maxBuy) return state.copy(message = "Not enough stash space.")
    
    item.qty -= maxBuy
    
    return state.copy(
        cash = state.cash - cost,
        inventory = addItem(state.inventory, item.def.name, VendorType.COMMODITIES, maxBuy, item.price),
        message = "Bought $maxBuy ${item.def.name} for $${cost}.",
    )
}

private fun sellCommodity(state: GameState, item: InventoryItem, qty: Int): GameState {
    if (qty <= 0) return state
    val local = state.currentVisit.commodities.firstOrNull { it.def.name == item.name }?.price
    if (local == null) return state.copy(message = "No buyers for ${item.name} here.")
    val saleQty = min(qty, item.quantity)
    val revenue = local * saleQty
    return state.copy(
        cash = state.cash + revenue,
        inventory = removeItems(state.inventory, item.name, saleQty),
        message = "Sold $saleQty ${item.name} for $${revenue}.",
    )
}

private fun buyWeapon(state: GameState, item: MarketWeapon): GameState {
    if (item.qty <= 0) return state.copy(message = "Out of stock.")
    if (state.cash < item.price) return state.copy(message = "Not enough cash.")
    
    item.qty -= 1
    
    return state.copy(
        cash = state.cash - item.price,
        inventory = addItem(state.inventory, item.def.name, VendorType.WEAPONS, 1, item.price),
        message = "Bought ${item.def.name} for $${item.price}.",
    )
}

private fun buyAmmo(state: GameState, item: MarketAmmo): GameState {
    if (state.cash < item.price) return state.copy(message = "Not enough cash.")
    
    return state.copy(
        cash = state.cash - item.price,
        inventory = addItem(state.inventory, item.ammoName, VendorType.WEAPONS, 1, item.price),
        message = "Bought 1 ${item.ammoName} for $${item.price}.",
    )
}

private fun buyArmor(state: GameState, item: MarketArmor): GameState {
    if (item.qty <= 0) return state.copy(message = "Out of stock.")
    if (state.cash < item.def.price) return state.copy(message = "Not enough cash.")
    
    item.qty -= 1
    
    val newDefense = max(state.armorDefense, item.def.defense)
    return state.copy(
        cash = state.cash - item.def.price,
        inventory = addItem(state.inventory, item.def.name, VendorType.ARMOR, 1, item.def.price),
        armorDefense = newDefense,
        message = "Bought ${item.def.name}. Defense is now ${newDefense}.",
    )
}

private fun sellArmor(state: GameState, item: InventoryItem): GameState {
    val armorDef = armorDefs.find { it.name == item.name } ?: return state
    val sellPrice = armorDef.price / 2
    val newInventory = removeOne(state.inventory, item.name)
    val newDefense = newInventory.filter { it.type == VendorType.ARMOR }
        .mapNotNull { inv -> armorDefs.find { it.name == inv.name }?.defense }
        .maxOrNull() ?: 0
    return state.copy(
        cash = state.cash + sellPrice,
        inventory = newInventory,
        armorDefense = newDefense,
        message = "Sold ${item.name} for $${sellPrice}. Defense is now ${newDefense}."
    )
}

private fun takeLoan(state: GameState, amount: Int): GameState {
    val newLoan = Loan(amount, state.day + LOAN_TERM_DAYS)
    val newLoans = state.mobster.loans + newLoan
    val oldestDueDay = newLoans.minOf { it.dueDay }
    
    return state.copy(
        cash = state.cash + amount,
        debt = state.debt + amount,
        mobster = state.mobster.copy(
            debtDueDay = oldestDueDay,
            lastPenaltyStage = 0,
            latestThreat = "Vinnie is counting the days. 'You got until day $oldestDueDay.'",
            loans = newLoans
        ),
        message = "Borrowed $${amount}. Oldest due day: $oldestDueDay.",
    )
}

private fun repayDebt(state: GameState, amount: Int): GameState {
    var remainingPayment = min(amount, min(state.cash, state.debt))
    if (remainingPayment <= 0) return state.copy(message = "No payment made.")
    
    val totalPaid = remainingPayment
    var activeLoans = state.mobster.loans.sortedBy { it.dueDay }.toMutableList()
    
    while (remainingPayment > 0 && activeLoans.isNotEmpty()) {
        val oldest = activeLoans[0]
        if (remainingPayment >= oldest.amount) {
            remainingPayment -= oldest.amount
            activeLoans.removeAt(0)
        } else {
            activeLoans[0] = oldest.copy(amount = oldest.amount - remainingPayment)
            remainingPayment = 0
        }
    }
    
    val newDebt = activeLoans.sumOf { it.amount }
    val nextDueDay = if (activeLoans.isNotEmpty()) activeLoans.minOf { it.dueDay } else state.day + LOAN_TERM_DAYS + 1
    
    val mobster = state.mobster.copy(
        debtDueDay = nextDueDay,
        latestThreat = if (newDebt == 0) "Vinnie counts the cash and backs off... for now." else "Vinnie pockets $${totalPaid}. You still owe $${newDebt} by day $nextDueDay.",
        loans = activeLoans
    )
    
    return state.copy(
        cash = state.cash - totalPaid, 
        debt = newDebt, 
        mobster = mobster, 
        message = "Paid Vinnie $${totalPaid}."
    )
}

private fun healPlayer(state: GameState, healAmt: Int): GameState {
    if (healAmt <= 0) return state
    val cost = healAmt * 100
    if (state.cash < cost) return state.copy(message = "Not enough cash for the clinic.")
    if (state.health >= 100) return state.copy(message = "You're already at full health.")
    val maxHeal = 100 - state.health
    if (healAmt > maxHeal) return state.copy(message = "You can't overheal above 100.")
    
    return state.copy(
        cash = state.cash - cost,
        health = state.health + healAmt,
        message = "The doctor patched you up for $$cost. Health is now ${state.health + healAmt}."
    )
}

private fun depositFunds(state: GameState, amount: Int): GameState {
    if (amount <= 0 || state.cash < amount) return state.copy(message = "Not enough cash to deposit.")
    return state.copy(
        cash = state.cash - amount,
        bankSavings = state.bankSavings + amount,
        message = "Deposited $$amount into savings."
    )
}

private fun withdrawFunds(state: GameState, amount: Int): GameState {
    if (amount <= 0 || state.bankSavings < amount) return state.copy(message = "Not enough savings to withdraw.")
    return state.copy(
        cash = state.cash + amount,
        bankSavings = state.bankSavings - amount,
        message = "Withdrew $$amount from savings."
    )
}

private fun applyMobsterPenalty(state: GameState): GameState {
    if (state.debt <= 0) return state
    val overdue = state.overdueDays()
    val stage = when {
        overdue >= 7 -> 4
        overdue >= 5 -> 3
        overdue >= 3 -> 2
        overdue >= 1 -> 1
        else -> 0
    }
    if (stage <= state.mobster.lastPenaltyStage) return state
    return when (stage) {
        1 -> {
            val dmg = 15
            val newHealth = max(0, state.health - dmg)
            state.copy(
                health = newHealth,
                mobster = state.mobster.copy(lastPenaltyStage = 1, latestThreat = "Day ${state.day}: Vinnie sends knuckleheads. They crack you for $dmg damage."),
                eventLog = "Vinnie's crew catches you 1 day late and roughs you up.",
                gameOver = newHealth <= 0,
                message = if (newHealth <= 0) "Vinnie's first warning finished you." else "You are now overdue. Pay Vinnie.",
                vinniePenaltyAmount = dmg,
            )
        }
        2 -> {
            val dmg = 25
            val newHealth = max(0, state.health - dmg)
            state.copy(
                health = newHealth,
                mobster = state.mobster.copy(lastPenaltyStage = 2, latestThreat = "Day ${state.day}: Vinnie personally smashes your ribs. $dmg damage."),
                eventLog = "3 days late. Vinnie shows up in person.",
                gameOver = newHealth <= 0,
                message = if (newHealth <= 0) "Vinnie took you out." else "Vinnie escalated the collection.",
                vinniePenaltyAmount = dmg,
            )
        }
        3 -> {
            val dmg = 35
            val newHealth = max(0, state.health - dmg)
            state.copy(
                health = newHealth,
                mobster = state.mobster.copy(lastPenaltyStage = 3, latestThreat = "Day ${state.day}: Vinnie leaves you bleeding in an alley. $dmg damage."),
                eventLog = "5 days late. Vinnie nearly kills you.",
                gameOver = newHealth <= 0,
                message = if (newHealth <= 0) "Vinnie finished the job." else "Barely alive. Pay him now.",
                vinniePenaltyAmount = dmg,
            )
        }
        else -> state.copy(
            health = 0,
            gameOver = true,
            mobster = state.mobster.copy(lastPenaltyStage = 4, latestThreat = "7 days late. Vinnie ends your run permanently."),
            eventLog = "7 days overdue. Vinnie ends your game.",
            message = "You stayed overdue too long.",
            vinniePenaltyAmount = 100,
        )
    }
}

private fun rollEncounter(state: GameState, nextVisit: CityVisit): EncounterState? {
    val rng = seededRandom(state.playerName, state.day, nextVisit.city.name, nextVisit.daySeed, state.cash, state.debt)
    if (rng.nextInt(100) >= 20) return null
    val carryingContraband = state.inventory.any { it.name == nextVisit.contraband && it.type == VendorType.COMMODITIES }
    // Roll encounter type: cops related (speeding, search, contraband sweep) vs thieves
    val roll = rng.nextInt(100)
    val type = when {
        roll < 25 -> EncounterType.SPEEDING
        roll < 50 -> EncounterType.VEHICLE_SEARCH
        carryingContraband && rng.nextBoolean() -> EncounterType.COPS
        else -> EncounterType.THIEVES
    }
    return when (type) {
        EncounterType.THIEVES -> EncounterState(
            type = EncounterType.THIEVES,
            title = "Stickup on the road",
            text = "Some grimy thieves try to snatch your stash. Run or fight.",
            cityContraband = nextVisit.contraband,
        )
        EncounterType.COPS -> EncounterState(
            type = EncounterType.COPS,
            title = "Contraband sweep",
            text = "Cops in ${nextVisit.city.name} are cracking down on ${nextVisit.contraband}. Run or fight.",
            cityContraband = nextVisit.contraband,
        )
        EncounterType.SPEEDING -> EncounterState(
            type = EncounterType.SPEEDING,
            title = "Pulled over!",
            text = "A cop pulls you over for speeding. 'That'll be a \$100 fine, pal. Pay up or we got a problem.'",
            cityContraband = nextVisit.contraband,
        )
        EncounterType.VEHICLE_SEARCH -> EncounterState(
            type = EncounterType.VEHICLE_SEARCH,
            title = "Vehicle search",
            text = "A cop pulls you over and demands to search your vehicle. 'Step out. We're gonna take a look.'",
            cityContraband = nextVisit.contraband,
        )
        else -> null
    }
}

private fun paySpeedingFine(state: GameState): GameState {
    val encounter = state.activeEncounter ?: return state
    if (state.cash < 100) return state.copy(message = "you dont have enough cash dude!")
    return state.copy(
        cash = state.cash - 100,
        activeEncounter = null,
        message = "Paid the \$100 speeding fine. Cops let you go.",
        eventLog = "Paid a \$100 speeding ticket."
    )
}

private fun acceptSearch(state: GameState): GameState {
    val encounter = state.activeEncounter ?: return state
    val hasDrugs = state.inventory.any { it.type == VendorType.COMMODITIES }
    return if (hasDrugs) {
        val newInventory = state.inventory.filter { it.type != VendorType.COMMODITIES }
        val fine = 500
        val newCash = max(0, state.cash - fine)
        state.copy(
            inventory = newInventory,
            cash = newCash,
            activeEncounter = null,
            message = "Cops found your stash! They confiscated all your drugs and fined you $500.",
            eventLog = "Lost all drugs and fined $500 in a vehicle search."
        )
    } else {
        state.copy(
            activeEncounter = null,
            message = "Cops searched your vehicle and found nothing. You're free to go.",
            eventLog = "Passed a vehicle search clean."
        )
    }
}

private fun bribeCop(state: GameState): GameState {
    if (state.cash < 5000) return state.copy(message = "you dont have enough cash dude! Bribe costs \$5000.")
    return state.copy(
        cash = state.cash - 5000,
        activeEncounter = null,
        message = "Slipped the cop \$5000. He looks the other way.",
        eventLog = "Bribed a cop for \$5000."
    )
}

private fun prepareTravelChoices(state: GameState): GameState {
    if (state.gameOver || !state.started || state.activeEncounter != null) return state
    val choices = cities.filter { it.name != state.currentVisit.city.name }
        .shuffled(seededRandom(state.day, state.cash, state.playerName, "choices"))
        .take(3)
        .map { it.name }
    return state.copy(travelChoices = choices, message = "Pick your next city. Choosing one ends the day.")
}

private fun travelToChoice(state: GameState, cityName: String): GameState {
    if (state.gameOver || !state.started || state.activeEncounter != null) return state
    val nextDay = state.day + 1
    if (nextDay > state.selectedLength.days) {
        val score = ScoreEntry(state.playerName, state.selectedLength.days, state.netWorth(), state.currentVisit.city.name, state.rankName())
        val updated = (state.leaderboard + score).sortedByDescending { it.finalNetWorth }.take(20)
        return state.copy(gameOver = true, leaderboard = updated, message = "Run complete. Final net worth: $${state.netWorth()}.", travelChoices = emptyList())
    }
    val nextCity = cities.firstOrNull { it.name == cityName } ?: return state
    val nextVisit = generateCityVisit(nextCity, nextDay)
    
    // Apply interest to each loan
    val updatedLoans = state.mobster.loans.map { it.copy(amount = (it.amount * 1.05).toInt()) }
    val newDebt = updatedLoans.sumOf { it.amount }
    
    var newState = state.copy(
        day = nextDay,
        cash = state.cash, // Bank savings interest is applied later
        debt = newDebt,
        currentVisit = nextVisit,
        mobster = state.mobster.copy(loans = updatedLoans, dailyBorrowed = 0),
        travelChoices = emptyList(),
        eventLog = "",
        message = "A new day begins in ${nextCity.name}.",
    )
    newState = applyMobsterPenalty(newState)
    if (newState.gameOver) return newState
    
    var encounter: EncounterState? = null
    if (newState.vinniePenaltyAmount == 0) {
        encounter = rollEncounter(newState, nextVisit)
        if (encounter == null && nextVisit.plummetTarget != null) {
        val snippet = listOf(
            "A shady merchant whispers in your ear. '${nextVisit.plummetTarget} is practically worthless today. The market just fell out. Stock up if you can hold it.'",
            "A twitchy guy in an alley pulls you aside. 'Hey, rumor has it everyone's dumping ${nextVisit.plummetTarget}. You can get it for dirt cheap right now.'",
            "You overhear some smugglers arguing. 'I'm telling you, ${nextVisit.plummetTarget} prices just tanked! The suppliers panicked. Buy it while it's low!'",
            "A hooded figure slides up next to you. 'Word on the street is ${nextVisit.plummetTarget} took a nosedive today. Get in before it normalizes.'"
        ).random(seededRandom(state.day, state.cash, "tip"))

        encounter = EncounterState(
            type = EncounterType.PLUMMET,
            title = "A street tip",
            text = snippet,
            cityContraband = nextVisit.contraband,
            playerTurnText = "Got it."
        )
    }
    if (encounter == null) {
        val rng = seededRandom(state.day, state.cash, "stash")
        if (rng.nextInt(100) < 15) {
            newState = newState.copy(
                stashCapacity = newState.stashCapacity + 5,
                activeEncounter = EncounterState(
                    type = EncounterType.FREE_STASH,
                    title = "Lucky break",
                    text = "You stumbled upon an abandoned storage unit! You snag some extra bags and straps. Stash capacity increased by 5.",
                    cityContraband = nextVisit.contraband,
                    playerTurnText = "Got it."
                ),
                eventLog = newState.eventLog + " Found abandoned storage unit. Stash capacity +5.",
                message = "Stash upgraded (+5) for free."
            )
        }
    }
}

// Apply 3% daily interest on bank savings
    val newBankSavings = if (newState.bankSavings > 0) (newState.bankSavings * 1.03).toInt() else newState.bankSavings
    
    newState = newState.copy(bankSavings = newBankSavings)
    
    return if (encounter != null) newState.copy(activeEncounter = encounter, message = encounter.title) else newState
}

private fun runFromEncounter(state: GameState): GameState {
    val encounter = state.activeEncounter ?: return state
    if (encounter.type == EncounterType.FREE_STASH || encounter.type == EncounterType.PLUMMET) {
        return state.copy(activeEncounter = null, message = "Continuing journey.")
    }
    val rng = seededRandom(state.day, encounter.title, state.cash, state.health)
    val isCopType = encounter.type == EncounterType.COPS || encounter.type == EncounterType.SPEEDING || encounter.type == EncounterType.VEHICLE_SEARCH
    val successChance = if (isCopType) 25 else 50
    val success = rng.nextInt(100) < successChance
    val escapedWho = if (isCopType) "cops" else "thieves"
    return if (success) {
        state.copy(activeEncounter = null, eventLog = "You escaped the $escapedWho.", message = "You got away.")
    } else {
        val damage = 15
        val newHealth = max(0, state.health - max(1, damage - state.armorDefense / 3))
        
        var newInventory = state.inventory
        var dropMessage = ""
        val willDrop = rng.nextInt(100) < 20
        if (willDrop && newInventory.isNotEmpty()) {
            val dropItem = newInventory.random(rng)
            val dropQty = max(1, rng.nextInt(1, max(2, dropItem.quantity / 2 + 1)))
            newInventory = if (dropItem.quantity <= dropQty) {
                newInventory.filter { it.name != dropItem.name }
            } else {
                newInventory.map { if (it.name == dropItem.name) it.copy(quantity = it.quantity - dropQty) else it }
            }
            dropMessage = " You also dropped $dropQty ${dropItem.name} while running."
        }

        state.copy(
            health = newHealth,
            inventory = newInventory,
            activeEncounter = state.activeEncounter.copy(playerTurnText = "You failed to run and took a hit.$dropMessage"),
            eventLog = "Failed to run. Took damage.$dropMessage",
            gameOver = newHealth <= 0,
            message = if (newHealth <= 0) "You died trying to run." else "They caught you. Fight or keep trying.",
        )
    }
}

private fun bestWeapon(state: GameState): WeaponDef? = weaponDefs
    .filter { state.weaponOwned(it) > 0 }
    .sortedByDescending { it.damage }
    .firstOrNull { it.ammoName == null || state.ammoFor(it) >= it.ammoPerShot }

private fun fightEncounter(state: GameState): GameState {
    val encounter = state.activeEncounter ?: return state
    val weapon = bestWeapon(state)
    val baseDamage = weapon?.damage ?: 5
    val actualDamage = if (encounter.type == EncounterType.COPS) max(1, baseDamage / 2) else baseDamage
    var inventory = state.inventory
    if (weapon?.ammoName != null) {
        inventory = removeOne(inventory, weapon.ammoName)
    }
    val enemyHealth = max(0, encounter.enemyHealth - actualDamage)
    if (enemyHealth == 0) {
        val reward = if (encounter.type == EncounterType.COPS) 1200 else 800
        return state.copy(
            cash = state.cash + reward,
            inventory = inventory,
            activeEncounter = null,
            eventLog = "Won the fight against ${if (encounter.type == EncounterType.COPS) "the cops" else "the thieves"}.",
            message = "Fight won. Looted $${reward}.",
        )
    }
    if (encounter.type == EncounterType.THIEVES) {
        val rng = seededRandom(state.day, state.playerName, "thief_attack", state.health)
        val willSteal = rng.nextInt(100) < 20
        if (willSteal) {
            val hasZipperPocket = state.inventory.any { it.name == "Zipper pocket" }
            if (hasZipperPocket) {
                return state.copy(
                    health = state.health, 
                    inventory = inventory,
                    activeEncounter = encounter.copy(enemyHealth = enemyHealth, playerTurnText = "You dealt $actualDamage damage. The thieves tried to rob you, but your Zipper pocket kept your stash safe!"),
                    gameOver = false,
                    message = "Fight continues. Robbery prevented!"
                )
            } else {
                var stash = inventory.filter { it.type == VendorType.COMMODITIES }
                if (stash.isNotEmpty()) {
                    var dropMessage = ""
                    val dropQty = max(1, rng.nextInt(1, 6)) // Steal 1-5 pieces
                    val dropItem = stash.random(rng)
                    val actualDrop = min(dropItem.quantity, dropQty)
                    inventory = if (dropItem.quantity <= actualDrop) {
                        inventory.filter { it.name != dropItem.name }
                    } else {
                        inventory.map { if (it.name == dropItem.name) it.copy(quantity = it.quantity - actualDrop) else it }
                    }
                    dropMessage = " The thieves snatched $actualDrop ${dropItem.name} from your pockets!"
                    
                    return state.copy(
                        health = state.health,
                        inventory = inventory,
                        activeEncounter = encounter.copy(enemyHealth = enemyHealth, playerTurnText = "You dealt $actualDamage damage.$dropMessage"),
                        gameOver = false,
                        message = "Fight continues. You were robbed."
                    )
                }
            }
        }
    }

    val enemyDamage = if (encounter.type == EncounterType.COPS) COP_DAMAGE else THIEF_DAMAGE
    val reduced = max(1, enemyDamage - state.armorDefense)
    val newHealth = max(0, state.health - reduced)
    return state.copy(
        health = newHealth,
        inventory = inventory,
        activeEncounter = encounter.copy(enemyHealth = enemyHealth, playerTurnText = "You dealt $actualDamage damage with ${weapon?.name ?: "your fists"}. They hit back for $reduced."),
        gameOver = newHealth <= 0,
        message = if (newHealth <= 0) "You lost the fight." else "Fight continues.",
    )
}

private fun saveScores(entries: List<ScoreEntry>): String {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(JSONObject().apply {
            put("playerName", e.playerName)
            put("days", e.days)
            put("finalNetWorth", e.finalNetWorth)
            put("city", e.city)
            put("rank", e.rank)
        })
    }
    return array.toString()
}

private fun loadScores(raw: String?): List<ScoreEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ScoreEntry(o.optString("playerName", "Dealer"), o.getInt("days"), o.getInt("finalNetWorth"), o.getString("city"), o.optString("rank", "Rookie Runner")))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveInventory(entries: List<InventoryItem>): String {
    val arr = JSONArray()
    entries.forEach { e ->
        arr.put(JSONObject().apply {
            put("name", e.name)
            put("type", e.type.name)
            put("quantity", e.quantity)
            put("avgCost", e.avgCost)
        })
    }
    return arr.toString()
}

private fun loadInventory(raw: String): List<InventoryItem> {
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(InventoryItem(o.getString("name"), VendorType.valueOf(o.getString("type")), o.getInt("quantity"), o.getInt("avgCost")))
            }
        }
    } catch (_: Exception) { emptyList() }
}

private fun saveMobster(m: MobsterState): String = JSONObject().apply {
    put("name", m.name)
    put("debtDueDay", m.debtDueDay)
    put("lastPenaltyStage", m.lastPenaltyStage)
    put("latestThreat", m.latestThreat)
    val loansArr = JSONArray()
    m.loans.forEach { loan ->
        loansArr.put(JSONObject().apply {
            put("amount", loan.amount)
            put("dueDay", loan.dueDay)
        })
    }
    put("loans", loansArr)
}.toString()

private fun loadMobster(raw: String): MobsterState {
    return try {
        val o = JSONObject(raw)
        val name = o.optString("name", "Vinnie \"The Knuckle\" Moretti")
        val due = o.optInt("debtDueDay", 11)
        val stage = o.optInt("lastPenaltyStage", 0)
        val borrowed = o.optInt("dailyBorrowed", 0)
        val threat = o.optString("latestThreat", "Vinnie is waiting.")
        
        val loans = mutableListOf<Loan>()
        val loansArr = o.optJSONArray("loans")
        if (loansArr != null) {
            for (i in 0 until loansArr.length()) {
                val lo = loansArr.getJSONObject(i)
                loans.add(Loan(lo.getInt("amount"), lo.getInt("dueDay")))
            }
        } else if (due > 0) {
            // Backward compatibility: create a loan from existing debtDueDay
            // Note: actual debt amount will be restored from state.debt in save/load loop if needed,
            // but here we just need to initialize. Actually state.debt is loaded separately.
            // Let's assume we'll fix up the amount in GameState init or just use a placeholder.
            // For now, if no loans array, and debtDueDay exists, create a single loan.
            // The actual debt amount will be set by the GameState constructor from the saved 'debt' field.
            // This is a temporary measure for migration.
            loans.add(Loan(0, due)) // Amount will be overwritten by state.debt
        }
        
        MobsterState(name, due, stage, borrowed, threat, loans)
    } catch (_: Exception) { 
        MobsterState(debtDueDay = 11) 
    }
}

private fun gameStateSaver() = listSaver<GameState, Any>(
    save = {
        listOf(
            it.started,
            it.gameOver,
            it.playerName,
            it.selectedLength.name,
            it.day,
            it.cash,
            it.debt,
            it.health,
            it.stashCapacity,
            it.armorDefense,
            saveInventory(it.inventory),
            it.currentVisit.city.name,
            it.message,
            it.eventLog,
            saveMobster(it.mobster),
            saveScores(it.leaderboard),
            saveEncounter(it.activeEncounter),
            it.mobster.dailyBorrowed,
        )
    },
    restore = {
        val day = it[4] as Int
        val city = cities.firstOrNull { c -> c.name == it[11] as String } ?: cities.first()
        val visit = generateCityVisit(city, day)
        GameState(
            started = it[0] as Boolean,
            gameOver = it[1] as Boolean,
            playerName = it[2] as String,
            selectedLength = GameLength.valueOf(it[3] as String),
            day = day,
            cash = it[5] as Int,
            debt = it[6] as Int,
            health = it[7] as Int,
            stashCapacity = it[8] as Int,
            armorDefense = it[9] as Int,
            inventory = loadInventory(it[10] as String),
            currentVisit = visit,
            message = it[12] as String,
            eventLog = it[13] as String,
            mobster = run {
                val debtVal = it[6] as Int
                val m = loadMobster(it[14] as String).copy(dailyBorrowed = (it.getOrNull(17) as? Int) ?: 0)
                if (m.loans.isEmpty() && debtVal > 0) {
                    m.copy(loans = listOf(Loan(debtVal, m.debtDueDay)))
                } else m
            },
            leaderboard = loadScores(it[15] as String),
            activeEncounter = loadEncounter(it[16] as String),
        )
    }
)

private fun saveEncounter(e: EncounterState?): String {
    if (e == null) return ""
    return JSONObject().apply {
        put("type", e.type.name)
        put("title", e.title)
        put("text", e.text)
        put("cityContraband", e.cityContraband)
        put("enemyHealth", e.enemyHealth)
        put("playerTurnText", e.playerTurnText)
    }.toString()
}

private fun loadEncounter(raw: String): EncounterState? {
    if (raw.isBlank()) return null
    return try {
        val o = JSONObject(raw)
        EncounterState(
            type = EncounterType.valueOf(o.getString("type")),
            title = o.getString("title"),
            text = o.getString("text"),
            cityContraband = o.getString("cityContraband"),
            enemyHealth = o.getInt("enemyHealth"),
            playerTurnText = o.optString("playerTurnText", ""),
        )
    } catch (_: Exception) { null }
}

@Composable
fun TheDopestDealsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("dopest_deals", Context.MODE_PRIVATE) }
    val loadedScores = remember { loadScores(prefs.getString("scores", null)) }
    fun gameStateToJson(s: GameState): String {
        val list = listOf(
            s.started, s.gameOver, s.playerName, s.selectedLength.name, s.day,
            s.cash, s.debt, s.health, s.stashCapacity, s.armorDefense,
            saveInventory(s.inventory), s.currentVisit.city.name, s.message, s.eventLog,
            saveMobster(s.mobster), saveScores(s.leaderboard), saveEncounter(s.activeEncounter),
            s.mobster.dailyBorrowed, s.vinniePenaltyAmount, s.bankSavings
        )
        return org.json.JSONArray(list).toString()
    }

    fun gameStateFromJson(json: String, loadedScores: List<ScoreEntry>): GameState {
        try {
            val a = org.json.JSONArray(json)
            val day = a.getInt(4)
            val city = cities.firstOrNull { it.name == a.getString(11) } ?: cities.first()
            return GameState(
                started = a.getBoolean(0), gameOver = a.getBoolean(1), playerName = a.getString(2),
                selectedLength = GameLength.valueOf(a.getString(3)), day = day, cash = a.getInt(5),
                debt = a.getInt(6), health = a.getInt(7), stashCapacity = a.getInt(8),
                armorDefense = a.getInt(9), inventory = loadInventory(a.getString(10)),
                currentVisit = generateCityVisit(city, day), message = a.getString(12),
                eventLog = a.getString(13), mobster = loadMobster(a.getString(14)).copy(dailyBorrowed = a.optInt(17, 0)),
                leaderboard = loadScores(a.getString(15)), activeEncounter = loadEncounter(a.getString(16)),
                vinniePenaltyAmount = a.optInt(18, 0), bankSavings = a.optInt(19, 0)
            )
        } catch (e: Exception) { return GameState(leaderboard = loadedScores) }
    }
    
    var state by remember { 
        val saved = prefs.getString("saved_state", null)
        mutableStateOf(if (saved != null) gameStateFromJson(saved, loadedScores) else GameState(leaderboard = loadedScores)) 
    }
    
    var isMuted by remember { mutableStateOf(prefs.getBoolean("is_muted", false)) }
    
    androidx.compose.runtime.LaunchedEffect(state) {
        prefs.edit().putString("saved_state", gameStateToJson(state)).apply()
    }

    var nameInput by remember { mutableStateOf(state.playerName) }

    fun persistScores(entries: List<ScoreEntry>) {
        prefs.edit().putString("scores", saveScores(entries)).apply()
    }

    fun beginRun() {
        val startCity = cities.random()
        state = GameState(
            started = true,
            playerName = nameInput.ifBlank { "Dealer" },
            selectedLength = state.selectedLength,
            day = 1,
            cash = 2000,
            debt = 2000,
            health = 100,
            stashCapacity = 30,
            armorDefense = 0,
            inventory = emptyList(),
            currentVisit = generateCityVisit(startCity, 1),
            message = "Run started in ${startCity.name} with only Vinnie's $2000 loan in your pocket.",
            eventLog = "",
            mobster = initialMobsterLoan(1),
            leaderboard = state.leaderboard,
        )
    }

    if (state.gameOver && state.started) {
        val score = ScoreEntry(state.playerName, state.selectedLength.days, state.netWorth(), state.currentVisit.city.name, state.rankName())
        val exists = state.leaderboard.any { it.playerName == score.playerName && it.days == score.days && it.finalNetWorth == score.finalNetWorth && it.city == score.city }
        if (!exists) {
            val updated = (state.leaderboard + score).sortedByDescending { it.finalNetWorth }.take(20)
            state = state.copy(leaderboard = updated)
            persistScores(updated)
        }
    }

    TravelSoundEffect(day = state.day, cityName = state.currentVisit.city.name, enabled = state.started && state.day > 1, isMuted = isMuted, hasEncounter = state.activeEncounter != null || state.currentVisit.plummetTarget != null || state.vinniePenaltyAmount > 0)

    var currentTab by rememberSaveable { mutableStateOf(0) }
    var marketTab by rememberSaveable { mutableStateOf(0) }
    var vinnieBorrowInput by rememberSaveable { mutableStateOf("") }
    var vinnieRepayInput by rememberSaveable { mutableStateOf("") }
    var bankDepositInput by rememberSaveable { mutableStateOf("") }
    var bankWithdrawInput by rememberSaveable { mutableStateOf("") }

    var showOptionsMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (state.started) {
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.TopAppBar(
                    title = { Text("Plugs n' Pushers", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF17202B)),
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color(0xFF2A3442))
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute Sounds" else "Mute Sounds", color = Color.White) },
                                onClick = {
                                    isMuted = !isMuted
                                    prefs.edit().putBoolean("is_muted", isMuted).apply()
                                    showOptionsMenu = false
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Restart Game", color = Color(0xFFEF9A9A)) },
                                onClick = {
                                    currentTab = 0
                                    state = GameState(leaderboard = state.leaderboard)
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (state.started) {
                NavigationBar(containerColor = Color(0xFF17202B)) {
                    NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Favorite, "") }, label = { Text("Status") })
                    NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Storefront, "") }, label = { Text("Market") })
                    NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.AttachMoney, "") }, label = { Text("Vinnie") })
                    NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.AccountBalance, "") }, label = { Text("Bank") })
                    NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.LocalHospital, "") }, label = { Text("Doctor") })
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E13), Color(0xFF151D28), Color(0xFF0E141B))))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.started) {
                HeroCard(state)
                SetupCard(state = state, nameInput = nameInput, onNameChange = { nameInput = it.take(18) }, onSelectLength = { state = state.copy(selectedLength = it) }, onStart = { beginRun() })
                LeaderboardCard(state.leaderboard)
            } else {
                when (currentTab) {
                    0 -> { // Status Tab
                        HeroCard(state, compact = true)
                        ActionCard(state = state, onTravel = { state = prepareTravelChoices(state) })
                        if (state.travelChoices.isNotEmpty() && state.activeEncounter == null) {
                            TravelChoiceCard(state) { choice -> state = travelToChoice(state, choice) }
                        }
                        StatusCard(state)
                        CityCard(state)
                    }
                    1 -> { // Market Tab (Merged with Stash)
                        InventoryCard(state) { item, qty -> state = sellCommodity(state, item, qty) }
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = marketTab == 0, onClick = { marketTab = 0 }, label = { Text("Drugs") })
                                FilterChip(selected = marketTab == 1, onClick = { marketTab = 1 }, label = { Text("Weapons") })
                                FilterChip(selected = marketTab == 2, onClick = { marketTab = 2 }, label = { Text("Armor") })
                            }
                            Text("Cash: $${state.cash}", color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                        }
                        if (state.message.isNotEmpty()) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723))) {
                                Text(state.message, color = Color(0xFFFFAB91), fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp).fillMaxWidth(), fontSize = 14.sp)
                            }
                        }
                        when (marketTab) {
                            0 -> CommodityVendorCard(state) { market, qty ->
                                val prev = state.cash
                                state = buyCommodity(state, market, qty)
                                if (state.message.contains("enough cash", ignoreCase = true) && !isMuted) {
                                    val mp = MediaPlayer.create(context, R.raw.perfectfart)
                                    mp?.let {
                                        val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                        enhancer.setTargetGain(600)
                                        enhancer.enabled = true
                                        it.setOnCompletionListener { player -> enhancer.release(); player.release() }
                                        it.start()
                                    }
                                }
                            }
                            1 -> WeaponVendorCard(state, onBuyWeapon = {
                                state = buyWeapon(state, it)
                                if (state.message.contains("enough cash", ignoreCase = true) && !isMuted) {
                                    val mp = MediaPlayer.create(context, R.raw.perfectfart)
                                    mp?.let {
                                        val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                        enhancer.setTargetGain(600)
                                        enhancer.enabled = true
                                        it.setOnCompletionListener { player -> enhancer.release(); player.release() }
                                        it.start()
                                    }
                                }
                            }, onBuyAmmo = {
                                state = buyAmmo(state, it)
                                if (state.message.contains("enough cash", ignoreCase = true) && !isMuted) {
                                    val mp = MediaPlayer.create(context, R.raw.perfectfart)
                                    mp?.let {
                                        val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                        enhancer.setTargetGain(600)
                                        enhancer.enabled = true
                                        it.setOnCompletionListener { player -> enhancer.release(); player.release() }
                                        it.start()
                                    }
                                }
                            })
                            2 -> ArmorVendorCard(state, onBuyArmor = {
                                state = buyArmor(state, it)
                                if (state.message.contains("enough cash", ignoreCase = true) && !isMuted) {
                                    val mp = MediaPlayer.create(context, R.raw.perfectfart)
                                    mp?.let {
                                        val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                        enhancer.setTargetGain(600)
                                        enhancer.enabled = true
                                        it.setOnCompletionListener { player -> enhancer.release(); player.release() }
                                        it.start()
                                    }
                                }
                            }, onSellArmor = { state = sellArmor(state, it) })
                        }
                    }
                    2 -> { // Vinnie Tab
                        MobsterCard(
                            state = state, 
                            borrowInput = vinnieBorrowInput,
                            onBorrowInputChange = { vinnieBorrowInput = it },
                            repayInput = vinnieRepayInput, 
                            onRepayInputChange = { vinnieRepayInput = it },
                            onBorrow = { amount -> 
                                state = takeLoan(state, amount) 
                                vinnieBorrowInput = ""
                            }, 
                            onRepay = { amount -> 
                                state = repayDebt(state, amount)
                                vinnieRepayInput = "" 
                            }
                        )
                    }
                    3 -> { // Banker Tab
                        BankerCard(
                            state = state,
                            depositInput = bankDepositInput,
                            onDepositInputChange = { bankDepositInput = it },
                            withdrawInput = bankWithdrawInput,
                            onWithdrawInputChange = { bankWithdrawInput = it },
                            onDeposit = { amount -> 
                                state = depositFunds(state, amount)
                                bankDepositInput = ""
                            },
                            onWithdraw = { amount -> 
                                state = withdrawFunds(state, amount)
                                bankWithdrawInput = ""
                            }
                        )
                    }
                    4 -> { // Doctor Tab
                        DoctorCard(state) { amt -> state = healPlayer(state, amt) }
                    }
                }
            }
        }
    }

    if (state.activeEncounter != null) {
        EncounterCard(state = state, onRun = {
            val oldHealth = state.health
            val newState = runFromEncounter(state)
            if (newState.health < oldHealth && !prefs.getBoolean("is_muted", false)) {
                MediaPlayer.create(context, R.raw.punch)?.start()
            }
            state = newState
        }, onFight = { state = fightEncounter(state) },
        onPayFine = { state = paySpeedingFine(state) },
        onAcceptSearch = { state = acceptSearch(state) },
        onBribe = { state = bribeCop(state) })
    }
    
    if (state.vinniePenaltyAmount > 0) {
        VinniePenaltyDialog(amount = state.vinniePenaltyAmount, isMuted = isMuted, onAcknowledge = { state = state.copy(vinniePenaltyAmount = 0) })
    }

    if (state.gameOver && state.started && state.vinniePenaltyAmount == 0) {
        GameOverDialog(state = state, onRetry = {
            currentTab = 0
            state = GameState(leaderboard = state.leaderboard)
        })
    }
}

@Composable
private fun HeroCard(state: GameState, compact: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2532)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!compact) {
                Text("Plugs n' Pushers", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Buy low. Work shady street-seller routes. Dodge cops. Survive Vinnie.", color = Color(0xFFE0E0E0))
            }
            Text("Current rank: ${state.rankName()}", color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold)
            if (compact && state.message.isNotEmpty()) {
                Text(state.message, color = Color(0xFFCFD8DC), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SetupCard(state: GameState, nameInput: String, onNameChange: (String) -> Unit, onSelectLength: (GameLength) -> Unit, onStart: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17202B))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = nameInput, onValueChange = onNameChange, label = { Text("Player name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameLength.values().forEach { len ->
                    FilterChip(selected = state.selectedLength == len, onClick = { onSelectLength(len) }, label = { Text("${len.days}d") })
                }
            }
            Button(onClick = onStart) { Text(if (state.started && !state.gameOver) "Restart Run" else "Start Run") }
            Text("Each new city offers 5-8 shady street-seller items, 1 contraband flag, a 20% encounter chance, and separate 35% weapon/armor vendor chances.", color = Color(0xFFB0BEC5))
        }
    }
}

@Composable
private fun StatusCard(state: GameState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE57373))
                Text("Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            StatLine("Dealer", state.playerName)
            StatLine("Day", "${state.day} / ${state.selectedLength.days}")
            StatLine("City", state.currentVisit.city.name)
            StatLine("Cash", "$${state.cash}")
            StatLine("Debt", "$${state.debt}")
            StatLine("Health", "${state.health}")
            StatLine("Armor", state.armorDefense.toString())
            StatLine("Stash", "${state.usedStash()} / ${state.stashCapacity}")
            StatLine("Net worth", "$${state.netWorth()}")
            if (state.eventLog.isNotEmpty()) {
                Text(state.eventLog, color = Color(0xFF90CAF9))
            }
        }
    }
}

@Composable
private fun MobsterCard(
    state: GameState, 
    borrowInput: String,
    onBorrowInputChange: (String) -> Unit,
    repayInput: String, 
    onRepayInputChange: (String) -> Unit, 
    onBorrow: (Int) -> Unit, 
    onRepay: (Int) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1F1F))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(id = R.drawable.vinnie_portrait),
                contentDescription = "Portrait of Vinnie the loan shark",
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFFFF8A65))
                Text("Vinnie", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            StatLine("Due day", state.mobster.debtDueDay.toString())
            StatLine("Past due", state.overdueDays().toString())
            HorizontalDivider(color = Color(0xFF3E2C2C))
            

            Text("He waits 10 full days before you are late. Penalties hit at 1, 3, 5, and 7 days overdue.", color = Color(0xFFEF9A9A), fontSize = 12.sp)
            
            if (!state.gameOver) {
                Text("Remaining daily allowance: $${max(0, 20000 - state.mobster.dailyBorrowed)}", color = Color(0xFFA5D6A7), fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val amountToBorrow = borrowInput.toIntOrNull() ?: 0
                    OutlinedTextField(
                        value = borrowInput,
                        onValueChange = { newValue -> if (newValue.all { it.isDigit() }) onBorrowInputChange(newValue) },
                        label = { Text("Borrow ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { 
                            onBorrow(amountToBorrow)
                            focusManager.clearFocus()
                        }, 
                        enabled = state.activeEncounter == null && amountToBorrow > 0, 
                        modifier = Modifier.weight(1f)
                    ) { Text("Borrow") }
                }
                if (state.debt > 0) {
                    Text("Total owed: $${state.debt}", color = Color(0xFFFFCCBC), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val amountToPay = repayInput.toIntOrNull() ?: 0
                        OutlinedTextField(
                            value = repayInput,
                            onValueChange = { newValue -> if (newValue.all { it.isDigit() }) onRepayInputChange(newValue) },
                            label = { Text("Amount ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { 
                                onRepay(amountToPay) 
                                focusManager.clearFocus()
                            }, 
                            enabled = state.cash > 0 && amountToPay > 0 && amountToPay <= state.cash && state.activeEncounter == null,
                            modifier = Modifier.weight(1f)
                        ) { Text("Pay") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(state: GameState, onHeal: (Int) -> Unit) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var amountInput by remember(state.health) { mutableStateOf(max(0, 100 - state.health).toString()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF222B27))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(id = R.drawable.doctor_portrait),
                contentDescription = "Portrait of shady street doctor",
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFF81C784))
                Text("Back-alley Clinic", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            val snark = remember(state.day) {
                listOf(
                    "\"No questions asked. Just cash on the table.\"",
                    "\"You look terrible. But for enough cash, I can fix that.\"",
                    "\"I don't care how you got shot, as long as you pay.\"",
                    "\"My license expired in '04, but my hands are steady.\""
                ).random()
            }
            Text(snark, color = Color(0xFFA5D6A7), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            HorizontalDivider(color = Color(0xFF2F3C36))
            
            val amountToHeal = amountInput.toIntOrNull() ?: 0
            val cost = amountToHeal * 100
            
            Text("Healing costs $100 per HP. You have ${state.health} / 100 health.", color = Color(0xFFD0D7DE))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { newValue -> if (newValue.all { it.isDigit() }) amountInput = newValue },
                    label = { Text("HP to Heal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { 
                        onHeal(amountToHeal)
                        focusManager.clearFocus()
                    }, 
                    enabled = !state.gameOver && state.activeEncounter == null && state.cash >= cost && amountToHeal > 0 && (state.health + amountToHeal) <= 100, 
                    modifier = Modifier.weight(1f)
                ) { Text("Heal ($$cost)") }
            }
        }
    }
}

@Composable
private fun BankerCard(
    state: GameState,
    depositInput: String, onDepositInputChange: (String) -> Unit,
    withdrawInput: String, onWithdrawInputChange: (String) -> Unit,
    onDeposit: (Int) -> Unit, onWithdraw: (Int) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2835))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(id = R.drawable.banker_portrait),
                contentDescription = "Portrait of a shady bank teller",
                modifier = Modifier.fillMaxWidth().height(250.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF81D4FA))
                Text("Underground Vault", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            val snark = remember(state.day) {
                listOf(
                    "\"We wash it, we hold it, we take a cut. Safest place in the city.\"",
                    "\"Your money makes money here. Just don't ask where it comes from.\"",
                    "\"We guarantee 3% daily returns. The feds guarantee 0. You do the math.\""
                ).random()
            }
            Text(snark, color = Color(0xFFB3E5FC), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            HorizontalDivider(color = Color(0xFF324156))
            
            Text("Available Cash: $${state.cash}", color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Your Savings: $${state.bankSavings}", color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Earns 3% compounded interest daily.", color = Color(0xFFCFD8DC), fontSize = 12.sp)

            if (!state.gameOver) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val amountToDeposit = depositInput.toIntOrNull() ?: 0
                    OutlinedTextField(
                        value = depositInput,
                        onValueChange = { newValue -> if (newValue.all { it.isDigit() }) onDepositInputChange(newValue) },
                        label = { Text("Deposit ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { 
                            onDeposit(amountToDeposit)
                            focusManager.clearFocus()
                        }, 
                        enabled = state.cash >= amountToDeposit && amountToDeposit > 0 && state.activeEncounter == null, 
                        modifier = Modifier.weight(0.7f)
                    ) { Text("Deposit") }
                    Button(
                        onClick = { 
                            onDeposit(state.cash)
                            focusManager.clearFocus()
                        },
                        enabled = state.cash > 0 && state.activeEncounter == null,
                        modifier = Modifier.weight(0.6f)
                    ) { Text("Max") }
                }

                if (state.bankSavings > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val amountToWithdraw = withdrawInput.toIntOrNull() ?: 0
                        OutlinedTextField(
                            value = withdrawInput,
                            onValueChange = { newValue -> if (newValue.all { it.isDigit() }) onWithdrawInputChange(newValue) },
                            label = { Text("Withdraw ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { 
                                onWithdraw(amountToWithdraw)
                                focusManager.clearFocus()
                            }, 
                            enabled = state.bankSavings >= amountToWithdraw && amountToWithdraw > 0 && state.activeEncounter == null, 
                            modifier = Modifier.weight(0.7f)
                        ) { Text("Withdraw") }
                        Button(
                            onClick = { 
                                onWithdraw(state.bankSavings)
                                focusManager.clearFocus()
                            },
                            enabled = state.bankSavings > 0 && state.activeEncounter == null,
                            modifier = Modifier.weight(0.6f)
                        ) { Text("Max") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityCard(state: GameState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2432))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF90CAF9))
                Text(state.currentVisit.city.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(state.currentVisit.city.summary, color = Color(0xFFD0D7DE))
            Text("Contraband in this city: ${state.currentVisit.contraband}", color = Color(0xFFFFAB91), fontWeight = FontWeight.Bold)
            Text(state.currentVisit.marketEventText, color = Color(0xFFA5D6A7), fontSize = 12.sp)
            Text("If you're carrying that item here, cops may show up during the 20% encounter roll.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
        }
    }
}

@Composable
private fun EncounterCard(state: GameState, onRun: () -> Unit, onFight: () -> Unit, onPayFine: () -> Unit = {}, onAcceptSearch: () -> Unit = {}, onBribe: () -> Unit = {}) {
    val encounter = state.activeEncounter ?: return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { }, 
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF301C1C))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (encounter.type == EncounterType.COPS || encounter.type == EncounterType.SPEEDING || encounter.type == EncounterType.VEHICLE_SEARCH) {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("dopest_deals", android.content.Context.MODE_PRIVATE)
                LaunchedEffect(encounter) {
                    if (!prefs.getBoolean("is_muted", false)) {
                        val mp = MediaPlayer.create(context, R.raw.sirens)
                        mp?.let {
                            val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                            enhancer.setTargetGain(1800)
                            enhancer.enabled = true
                            it.setOnCompletionListener { player ->
                                enhancer.release()
                                player.release()
                            }
                            it.start()
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.cop_portrait),
                        contentDescription = "Portrait of corrupt cop",
                        modifier = Modifier.weight(0.34f)
                    )
                    Column(modifier = Modifier.weight(0.66f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocalPolice, contentDescription = null, tint = Color(0xFFFFCC80))
                            Text(encounter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text(encounter.text, color = Color(0xFFFFE0B2))
                    }
                }
                HorizontalDivider(color = Color(0xFF452828))
            } else if (encounter.type == EncounterType.THIEVES) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.thief_portrait),
                        contentDescription = "Portrait of a grimy thief",
                        modifier = Modifier.weight(0.34f)
                    )
                    Column(modifier = Modifier.weight(0.66f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFCC80))
                            Text(encounter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text(encounter.text, color = Color(0xFFFFE0B2))
                    }
                }
                HorizontalDivider(color = Color(0xFF452828))
            } else if (encounter.type == EncounterType.FREE_STASH) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.stash_upgrade_man),
                        contentDescription = "Portrait of man finding stash",
                        modifier = Modifier.weight(0.34f)
                    )
                    Column(modifier = Modifier.weight(0.66f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFA5D6A7))
                            Text(encounter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text(encounter.text, color = Color(0xFFC8E6C9))
                    }
                }
                HorizontalDivider(color = Color(0xFF452828))
            } else if (encounter.type == EncounterType.PLUMMET) {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("dopest_deals", android.content.Context.MODE_PRIVATE)
                LaunchedEffect(Unit) {
                    if (!prefs.getBoolean("is_muted", false)) {
                        val tipSounds = listOf(R.raw.guesswhat, R.raw.overhere, R.raw.ahem)
                        val mp = MediaPlayer.create(context, tipSounds.random())
                        mp?.let {
                            val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                            enhancer.setTargetGain(1800)
                            enhancer.enabled = true
                            it.setOnCompletionListener { player ->
                                enhancer.release()
                                player.release()
                            }
                            it.start()
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.commodity_plummet),
                        contentDescription = "Portrait of shady merchant",
                        modifier = Modifier.weight(0.34f)
                    )
                    Column(modifier = Modifier.weight(0.66f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFBCAAA4))
                            Text(encounter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text(encounter.text, color = Color(0xFFD7CCC8))
                    }
                }
                HorizontalDivider(color = Color(0xFF452828))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFCC80))
                    Text(encounter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Text(encounter.text, color = Color(0xFFFFE0B2))
            }
            
            when (encounter.type) {
                EncounterType.SPEEDING -> {
                    Text("Running from cops is risky — only 25% chance to escape.", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                    if (encounter.playerTurnText.isNotBlank()) Text(encounter.playerTurnText, color = Color(0xFF90CAF9))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPayFine, enabled = !state.gameOver) { Text("Pay \$100") }
                        Button(onClick = onRun, enabled = !state.gameOver) { Text("Run") }
                        Button(onClick = onFight, enabled = !state.gameOver) { Text("Fight") }
                    }
                }
                EncounterType.VEHICLE_SEARCH -> {
                    Text("Running from cops is risky — only 25% chance to escape.", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                    if (encounter.playerTurnText.isNotBlank()) Text(encounter.playerTurnText, color = Color(0xFF90CAF9))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = onAcceptSearch, enabled = !state.gameOver) { Text("Accept") }
                        Button(onClick = onBribe, enabled = !state.gameOver) { Text("Bribe \$5k") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRun, enabled = !state.gameOver) { Text("Run") }
                        Button(onClick = onFight, enabled = !state.gameOver) { Text("Fight") }
                    }
                }
                EncounterType.COPS, EncounterType.THIEVES -> {
                    StatLine("Enemy health", encounter.enemyHealth.toString())
                    Text(if (encounter.type == EncounterType.COPS) "Cops take half damage. 25% run chance." else "Thieves take full damage.", color = Color(0xFFB0BEC5))
                    if (encounter.playerTurnText.isNotBlank()) Text(encounter.playerTurnText, color = Color(0xFF90CAF9))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRun, enabled = !state.gameOver) { Text("Run") }
                        Button(onClick = onFight, enabled = !state.gameOver) { Text("Fight") }
                    }
                }
                EncounterType.FREE_STASH -> {
                    Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                }
                EncounterType.PLUMMET -> {
                    Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) { Text("Sweet, Thanks") }
                }
            }
        }
    }
    }
}

@Composable
private fun ActionCard(state: GameState, onTravel: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17202B))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Actions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Button(onClick = onTravel, enabled = !state.gameOver && state.activeEncounter == null, modifier = Modifier.fillMaxWidth()) { Text("Travel / New Day") }
        }
    }
}

@Composable
private fun CommodityVendorCard(state: GameState, onBuy: (MarketCommodity, Int) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("dopest_deals", Context.MODE_PRIVATE)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16222E))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF80DEEA))
                    Text("Street Dealers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Text("Stash: ${state.stashCapacity - state.freeStash()}/${state.stashCapacity}", color = Color(0xFFD0D7DE), fontWeight = FontWeight.Medium)
            }
            state.currentVisit.commodities.forEach { item ->
                androidx.compose.runtime.key(item.def.name) {
                    var qtyInput by remember { mutableStateOf("1") }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.def.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("$${item.price} • Stock: ${item.qty}", color = Color(0xFFB3E5FC))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qtyInput,
                                onValueChange = { str -> qtyInput = str.filter { it.isDigit() } },
                                modifier = Modifier.width(60.dp).height(50.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Button(
                                onClick = { 
                                    val qty = qtyInput.toIntOrNull() ?: 0
                                    val safeQty = min(qty, item.qty)
                                    if (safeQty > 0) {
                                        if (state.cash >= (item.price * safeQty) && state.freeStash() >= safeQty && !prefs.getBoolean("is_muted", false)) {
                                            val buySounds = listOf(R.raw.thatsall, R.raw.didntgetit, R.raw.ezmoney, R.raw.thankyou1)
                                            val mp = MediaPlayer.create(context, buySounds.random())
                                            mp?.let {
                                                val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                                enhancer.setTargetGain(1100)
                                                enhancer.enabled = true
                                                it.setOnCompletionListener { player ->
                                                    enhancer.release()
                                                    player.release()
                                                }
                                                it.start()
                                            }
                                        }
                                        onBuy(item, safeQty)
                                        qtyInput = "1"
                                        focusManager.clearFocus()
                                    }
                                }, 
                                enabled = item.qty > 0 && !state.gameOver && state.activeEncounter == null
                            ) { Text("Buy") }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2A3442))
                }
            }
        }
    }
}

@Composable
private fun WeaponVendorCard(state: GameState, onBuyWeapon: (MarketWeapon) -> Unit, onBuyAmmo: (MarketAmmo) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E2C))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFCC80))
                Text("Weapon vendor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (!state.currentVisit.weaponsAvailable) {
                Text("No weapon vendor in town.", color = Color(0xFFCFD8DC))
            } else {
                state.currentVisit.weaponStock.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.def.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Damage ${item.def.damage} • $${item.price}", color = Color(0xFFB3E5FC))
                        }
                        Button(onClick = { onBuyWeapon(item) }, enabled = item.qty > 0 && state.cash >= item.price && !state.gameOver && state.activeEncounter == null) { Text("Buy") }
                    }
                    val ammo = state.currentVisit.ammoStock.firstOrNull { it.weaponName == item.def.name }
                    if (ammo != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${ammo.ammoName} • $${ammo.price} ea", color = Color(0xFFD7CCC8))
                            Button(onClick = { onBuyAmmo(ammo) }, enabled = state.cash >= ammo.price && !state.gameOver && state.activeEncounter == null) { Text("Buy 1") }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2A3442))
                }
            }
        }
    }
}

@Composable
private fun ArmorVendorCard(state: GameState, onBuyArmor: (MarketArmor) -> Unit, onSellArmor: (InventoryItem) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2026))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Armor vendor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (!state.currentVisit.armorAvailable) {
                Text("No armor vendor in town.", color = Color(0xFFCFD8DC))
            } else {
                state.currentVisit.armorStock.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.def.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            val fannyPackText = if (item.def.name == "Fanny pack") "\nPrevents Stash Theft" else ""
                            Text("Defense ${item.def.defense} • $${item.def.price}$fannyPackText", color = Color(0xFFB3E5FC))
                        }
                        Button(onClick = { onBuyArmor(item) }, enabled = item.qty > 0 && state.cash >= item.def.price && !state.gameOver && state.activeEncounter == null) { Text("Buy") }
                    }
                    HorizontalDivider(color = Color(0xFF2A3442))
                }
                
                val ownedArmor = state.inventory.filter { it.type == VendorType.ARMOR }
                if (ownedArmor.isNotEmpty()) {
                    Text("Your Armor", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    ownedArmor.forEach { item ->
                        val armorDef = armorDefs.find { it.name == item.name }
                        if (armorDef != null) {
                            val sellPrice = armorDef.price / 2
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Sell for $${sellPrice}", color = Color(0xFFB3E5FC))
                                }
                                Button(onClick = { onSellArmor(item) }, enabled = !state.gameOver && state.activeEncounter == null) { Text("Sell") }
                            }
                            HorizontalDivider(color = Color(0xFF2A3442))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(state: GameState, onSell: (InventoryItem, Int) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("dopest_deals", Context.MODE_PRIVATE)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFA5D6A7))
                Text("Inventory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (state.inventory.isEmpty()) {
                Text("Nothing in inventory.", color = Color(0xFFCFD8DC))
            } else {
                state.inventory.forEach { item ->
                    androidx.compose.runtime.key(item.name) {
                        var qtyInput by remember { mutableStateOf("1") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.name} x${item.quantity}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Avg cost $${item.avgCost}", color = Color(0xFFB0BEC5))
                            }
                            if (item.type == VendorType.COMMODITIES) {
                                val localPrice = state.currentVisit.commodities.firstOrNull { it.def.name == item.name }?.price
                                if (localPrice != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = qtyInput,
                                            onValueChange = { str -> qtyInput = str.filter { it.isDigit() } },
                                            modifier = Modifier.width(60.dp).height(50.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                        Button(
                                            onClick = { 
                                                val qty = qtyInput.toIntOrNull() ?: 0
                                                if (qty > 0) {
                                                    if (!prefs.getBoolean("is_muted", false)) {
                                                        val sellSounds = listOf(R.raw.goodstuff, R.raw.gotanymore, R.raw.heythanksman, R.raw.awesomebro)
                                                        val mp = MediaPlayer.create(context, sellSounds.random())
                                            mp?.let {
                                                val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                                                enhancer.setTargetGain(1100)
                                                enhancer.enabled = true
                                                it.setOnCompletionListener { player ->
                                                    enhancer.release()
                                                    player.release()
                                                }
                                                it.start()
                                            }
                                                    }
                                                    onSell(item, qty)
                                                    qtyInput = "1"
                                                    focusManager.clearFocus()
                                                }
                                            }, 
                                            enabled = !state.gameOver && state.activeEncounter == null
                                        ) { Text("Sell ($$localPrice)") }
                                    }
                                } else {
                                    Button(onClick = { }, enabled = false) { Text("No buyers here") }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF2A3442))
                    }
                }
            }
            val best = bestWeapon(state)
            Text("Best fight weapon ready: ${best?.name ?: "None"}", color = Color(0xFF90CAF9))
        }
    }
}


@Composable
private fun TravelChoiceCard(state: GameState, onChoose: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2A22))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.travelChoices.forEach { city ->
                Button(onClick = { onChoose(city) }, enabled = !state.gameOver) { Text(city) }
            }
        }
    }
}

@Composable
private fun LeaderboardCard(entries: List<ScoreEntry>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202A39))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD54F))
                Text("Leaderboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (entries.isEmpty()) {
                Text("No scores yet.", color = Color(0xFFCFD8DC))
            } else {
                entries.take(10).forEachIndexed { index, entry ->
                    Text("#${index + 1} ${entry.playerName} • $${entry.finalNetWorth} • ${entry.days}d • ${entry.rank} • ${entry.city}", color = Color(0xFFD0D7DE))
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFB0BEC5))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OptionsCard(isMuted: Boolean, onMuteToggle: () -> Unit, onRestart: () -> Unit) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray)
                Text("Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text("Restart Game")
            }
            Button(onClick = onMuteToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (isMuted) "Unmute Sounds" else "Mute Sounds")
            }
            Button(onClick = { (context as? android.app.Activity)?.finish() }, modifier = Modifier.fillMaxWidth()) {
                Text("Exit Game")
            }
        }
    }
}

@Composable
private fun VinniePenaltyDialog(amount: Int, isMuted: Boolean, onAcknowledge: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(amount) {
        if (!isMuted) {
            val vinnieSounds = listOf(R.raw.wheresmymoney, R.raw.getemboys, R.raw.littletoe, R.raw.gotmymoney)
            val mp = MediaPlayer.create(context, vinnieSounds.random())
            mp?.let {
                val enhancer = android.media.audiofx.LoudnessEnhancer(it.audioSessionId)
                enhancer.setTargetGain(1800)
                enhancer.enabled = true
                it.setOnCompletionListener { player ->
                    enhancer.release()
                    player.release()
                }
                it.start()
            }
        }
    }
    val isFatal = amount >= 100
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { }, 
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF301C1C))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.vinnie_portrait),
                        contentDescription = "Vinnie Moretti",
                        modifier = Modifier.weight(0.34f)
                    )
                    Column(modifier = Modifier.weight(0.66f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350))
                            Text(if (isFatal) "Final Collection" else "Vinnie's Collection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text(if (isFatal) "Vinnie has run out of patience. He ends your run permanently." else "You're late on your payments. Vinnie's guys roughed you up.", color = Color(0xFFFFCC80))
                    }
                }
                Text(if (isFatal) "Vinnie collected in blood. Game Over." else "Took $amount damage. Pay your debt before it happens again.", color = Color(0xFFCFD8DC))
                Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isFatal) "Accept Fate" else "Understood")
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(state: GameState, onRetry: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { }, 
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
                Text("GAME OVER", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                val isDeath = state.health <= 0
                Text(
                    text = if (isDeath) "Your health depleted entirely. You didn't make it to the end of your run." else "You successfully completed your ${state.selectedLength.days} day run! Your final net worth is $${state.netWorth()}.",
                    color = Color(0xFFCFD8DC), 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry / Start New Run")
                }
            }
        }
    }
}
