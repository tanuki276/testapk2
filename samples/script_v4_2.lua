-- 脚本 v4.2

local TITLE = "脚本 v4.2"

local features = {
    { name = "体力无限", search = 100, value = 999999, enabled = false },
    { name = "武器连射", search = 200, value = 1, enabled = false },
    { name = "空中浮游", search = 300, value = 777, enabled = false },
    { name = "无限跳跃", search = 400, value = 9999, enabled = false },
    { name = "高速移动", search = 500, value = 2500, enabled = false }
}

local labels = {
    "❤️ 体力无限",
    "🔫 武器连射",
    "☁️ 空中浮游",
    "🦘 无限跳跃",
    "⚡ 高速移动"
}

local function states()
    local out = {}
    for i, f in ipairs(features) do out[i] = f.enabled end
    return out
end

local function enableFeature(f)
    gg.clearResults()
    gg.searchNumber(tostring(f.search), gg.TYPE_DWORD)

    local count = gg.getResultsCount()
    if count <= 0 then
        gg.toast("⚠️ " .. f.name .. "：未找到目标值")
        return false
    end

    local results = gg.getResults(count)
    for i, item in ipairs(results) do
        item.value = f.value
        item.freeze = true
    end

    gg.setValues(results)
    gg.addListItems(results)
    f.enabled = true
    gg.toast("✅ " .. f.name .. "：已开启")
    return true
end

local function disableFeature(f)
    -- Current simulator keeps the value but removes the local ON state.
    -- The UI immediately returns to the disabled color.
    f.enabled = false
    gg.toast("⛔ " .. f.name .. "：已关闭")
end

local function toggleFeature(f)
    if f.enabled then
        disableFeature(f)
    else
        enableFeature(f)
    end
end

local running = true

while running do
    local selected = gg.toggleMenu(labels, states(), TITLE)

    -- X closes only the menu. Script stays alive and waits for the floating icon.
    if selected == nil then
        gg.setVisible(false)
        while not gg.isVisible() do
            gg.sleep(100)
        end
        gg.setVisible(false)
    elseif selected >= 1 and selected <= #features then
        toggleFeature(features[selected])
        -- No menu recreation here: the same overlay stays open and the app
        -- updates the tapped row's background color in place.
    end
end
