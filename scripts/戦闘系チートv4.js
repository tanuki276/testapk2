// 战斗类作弊 v.4
(function () {
  const state = {
    浮空: false,
    高速移动: false,
    无敌: false,
    无限跳跃: false
  };

  document.body.innerHTML = `
    <div style="font-family:sans-serif;background:#0a0e12;color:#e8f0f7;padding:18px;min-height:100vh;box-sizing:border-box">
      <div style="font-size:22px;font-weight:800;letter-spacing:1px">战斗类作弊 v.4</div>
      <div style="color:#8fa1b3;font-size:12px;margin:5px 0 16px">TITAN CORE</div>
      <div id="status" style="padding:10px;border:1px solid #2a3948;border-radius:8px;margin-bottom:12px">● 已加载</div>
      <div id="buttons"></div>
      <button id="reset" style="width:100%;margin-top:14px;padding:13px;border:1px solid #2a3948;border-radius:8px;background:#18212b;color:#e8f0f7;font-size:15px">全部关闭</button>
    </div>`;

  const buttons = document.getElementById("buttons");
  const status = document.getElementById("status");

  Object.keys(state).forEach(function (name) {
    const button = document.createElement("button");
    button.style.cssText = "display:block;width:100%;margin:8px 0;padding:14px;border:1px solid #2a3948;border-radius:8px;background:#121820;color:#e8f0f7;text-align:left;font-size:16px";
    button.textContent = name + "　[关闭]";
    button.onclick = function () {
      state[name] = !state[name];
      button.textContent = name + "　[" + (state[name] ? "开启" : "关闭") + "]";
      button.style.borderColor = state[name] ? "#5be58b" : "#2a3948";
      status.textContent = "● " + name + " → " + (state[name] ? "开启" : "关闭");
      log("[GUI] " + name + " = " + (state[name] ? "开启" : "关闭"));
    };
    buttons.appendChild(button);
  });

  document.getElementById("reset").onclick = function () {
    Object.keys(state).forEach(function (name, i) {
      state[name] = false;
      buttons.children[i].textContent = name + "　[关闭]";
      buttons.children[i].style.borderColor = "#2a3948";
    });
    status.textContent = "● 全部关闭";
    log("[GUI] 全部功能已关闭");
  };

  log("=== 战斗类作弊 v.4 ===");
  log("浮空 / 高速移动 / 无敌 / 无限跳跃");
})();
