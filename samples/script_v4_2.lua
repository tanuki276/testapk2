-- 脚本 v4.2

local TITLE = "脚本 v4.2"

local function applyFeature(searchValue, newValue, label)
    gg.clearResults()
    gg.searchNumber(tostring(searchValue), gg.TYPE_DWORD)

    local count = gg.getResultsCount()
    if count <= 0 then
        gg.toast(label .. "：未找到目标值")
        return
    end

    local results = gg.getResults(count)
    for i, item in ipairs(results) do
        item.value = newValue
        item.freeze = true
    end

    gg.setValues(results)
    gg.addListItems(results)
    gg.toast(label .. "：已开启")
end

local function mainMenu()
    local menu = gg.choice({
        "❤️ 体力无限",
        "🔫 武器连射",
        "☁️ 空中浮游",
        "🦘 无限跳跃",
        "⚡ 高速移动",
        "🧩 全部开启",
        "👁 隐藏菜单",
        "❌ 退出脚本"
    }, nil, TITLE)

    if menu == nil or menu == 7 then
        gg.setVisible(false)
        return true
    elseif menu == 1 then
        applyFeature(100, 999999, "体力无限")
    elseif menu == 2 then
        applyFeature(200, 1, "武器连射")
    elseif menu == 3 then
        applyFeature(300, 777, "空中浮游")
    elseif menu == 4 then
        applyFeature(400, 9999, "无限跳跃")
    elseif menu == 5 then
        applyFeature(500, 2500, "高速移动")
    elseif menu == 6 then
        applyFeature(100, 999999, "体力无限")
        applyFeature(200, 1, "武器连射")
        applyFeature(300, 777, "空中浮游")
        applyFeature(400, 9999, "无限跳跃")
        applyFeature(500, 2500, "高速移动")
        gg.toast("全部功能已开启")
    elseif menu == 8 then
        gg.toast("脚本已退出")
        return false
    end

    return true
end

-- 第一次运行时直接显示菜单。
local running = mainMenu()
gg.setVisible(false)

-- 菜单关闭后保持脚本运行。
-- 再次点击浮动图标时重新打开菜单。
while running do
    if gg.isVisible() then
        gg.setVisible(false)
        running = mainMenu()
    end
    gg.sleep(100)
end
