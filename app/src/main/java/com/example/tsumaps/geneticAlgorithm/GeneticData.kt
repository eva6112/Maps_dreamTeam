package com.example.tsumaps.geneticAlgorithm


import java.time.LocalTime

data class Shop(
    val id: Int,
    val name: String,
    val x: Int,
    val y: Int,
    val items: Set<String>,
    val openTime: Int,
    val closeTime: Int
)

object ShopRegistry {
    private fun String.toMins(): Int {
        val parts = this.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    val shops = listOf(
        Shop(1, "Ярче! ", 148, 8, setOf("Кофе","Snickers","Пирожное Обычное", "Слойка с ветчиной и сыром", "Батончик NutGo", "Онигири","Пластиковая посуда","Йогурт","Киндер Буэно","Сочень с творогом",
            "Влажные салфетки","Orbit","Яблоки", "Вода", "Печенье Юбилейное",),
            "07:00".toMins(), "23:00".toMins()),
        Shop(2, "Абрикос", 3, 7, setOf("Яблоки","Эклер","Торт", "Snickers","Кофе","Вода", "Печенье Юбилейное","Пластиковая посуда","Готовая еда","Батончик NutGo","Киндер Буэно","Онигири","Йогурт", "Сыр", "Печенье Томское"),
            "08:00".toMins(), "23:00".toMins()),
        Shop(3, "Столовая ГК", 110, 60, setOf("Горячая еда", "Кофе Растворимое", "Чай","Пирожное Обычное"), "09:00".toMins(), "17:00".toMins()),
        Shop(4, "Baba Roma", 119, 14, setOf("Кофе","Слойка с ветчиной и сыром", "Сэндвич","Эклер","Торт","Пирожное","Готовая еда"),
            "08:00".toMins(), "21:00".toMins()),
        Shop(5, "Белка", 107, 14, setOf("Кофе", "Snickers"), "08:30".toMins(), "20:00".toMins()),
        Shop(6, "Starbooks", x = 61, 47, setOf("Кофе"), "08:00".toMins(), "19:00".toMins()),
        Shop(7, "Сибирские блины", 64, 44, setOf("Кофе", "Блин с вишней","Блин с ветчиной и сыром","Блин с Творогом","Блин Цезарь","Блин Деревенский"),
            "08:00".toMins(), "20:00".toMins()),
        Shop(8, "Столовая 2 корп.", 47, 63, setOf("Сочень с творогом", "Блин с ветчиной и сыром","Блин с Творогом","Кофе","Бургер","Батончик NutGo","Киндер Буэно", "Печенье Юбилейное", "Онигири"),
            "09:00".toMins(), "17:00".toMins()),
        Shop(9, "Xo bakery", 48, 71, setOf("Пирожное", "Торт","Сочень с творогом",), "08:00".toMins(), "21:00".toMins())
    )
}