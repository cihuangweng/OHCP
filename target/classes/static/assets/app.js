const state = {
    sessionId: localStorage.getItem("agroqa.sessionId") || crypto.randomUUID(),
    knowledge: [],
    filter: "全部",
    busy: false
};

localStorage.setItem("agroqa.sessionId", state.sessionId);

const chat = document.querySelector("#chat");
const form = document.querySelector("#questionForm");
const input = document.querySelector("#questionInput");
const sendButton = document.querySelector("#sendButton");
const sessionLabel = document.querySelector("#sessionLabel");
const knowledgeCount = document.querySelector("#knowledgeCount");
const knowledgeList = document.querySelector("#knowledgeList");
const historyList = document.querySelector("#historyList");

sessionLabel.textContent = `会话 ${state.sessionId.slice(0, 8)}`;

document.querySelectorAll("[data-question]").forEach((button) => {
    button.addEventListener("click", () => {
        input.value = button.dataset.question;
        input.focus();
    });
});

document.querySelectorAll(".filter").forEach((button) => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".filter").forEach((item) => item.classList.remove("active"));
        button.classList.add("active");
        state.filter = button.dataset.filter;
        renderKnowledge();
    });
});

document.querySelector("#clearHistory").addEventListener("click", async () => {
    await fetch(`/api/history?sessionId=${encodeURIComponent(state.sessionId)}`, {method: "DELETE"});
    chat.innerHTML = "";
    renderWelcome();
    await loadHistory();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const question = input.value.trim();
    if (!question || state.busy) {
        return;
    }
    appendMessage("你", question, "user");
    input.value = "";
    setBusy(true);
    try {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({sessionId: state.sessionId, question})
        });
        const data = await response.json();
        appendAssistant(data);
        await loadHistory();
    } catch (error) {
        appendMessage("系统", `请求失败：${error.message}`, "assistant");
    } finally {
        setBusy(false);
    }
});

input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
        form.requestSubmit();
    }
});

async function init() {
    renderWelcome();
    await Promise.all([loadKnowledge(), loadHistory()]);
}

function renderWelcome() {
    appendMessage(
        "助手",
        "可以输入作物名称、异常部位和症状。我会先从本地知识库检索相关病虫害，再给出判断依据、防治方案和参考来源。",
        "assistant"
    );
}

async function loadKnowledge() {
    const response = await fetch("/api/knowledge");
    state.knowledge = await response.json();
    knowledgeCount.textContent = `${state.knowledge.length} 条`;
    renderKnowledge();
}

async function loadHistory() {
    const response = await fetch(`/api/history?sessionId=${encodeURIComponent(state.sessionId)}`);
    const history = await response.json();
    renderHistory(history);
}

function renderKnowledge() {
    const items = state.filter === "全部"
        ? state.knowledge
        : state.knowledge.filter((item) => item.category === state.filter);
    knowledgeList.innerHTML = "";
    items.forEach((item) => {
        const button = document.createElement("button");
        button.className = "knowledge-item";
        button.innerHTML = `
            <strong>${escapeHtml(item.name)}</strong>
            <span>${escapeHtml(item.category)} · ${escapeHtml(item.crops)}</span>
            <p>${escapeHtml(item.symptoms)}</p>
        `;
        button.addEventListener("click", () => {
            input.value = `${item.name}的症状和防治方法是什么？`;
            input.focus();
        });
        knowledgeList.appendChild(button);
    });
}

function renderHistory(history) {
    historyList.innerHTML = "";
    if (!history.length) {
        historyList.className = "history-list empty";
        historyList.textContent = "暂无历史";
        return;
    }
    historyList.className = "history-list";
    [...history].reverse().forEach((message) => {
        const button = document.createElement("button");
        button.className = "history-entry";
        button.innerHTML = `
            <strong>${escapeHtml(message.question)}</strong>
            <span>${escapeHtml(message.intent)} · 置信度 ${Math.round(message.confidence * 100)}%</span>
        `;
        button.addEventListener("click", () => {
            appendMessage("你", message.question, "user");
            appendAssistant(message);
        });
        historyList.appendChild(button);
    });
}

function appendAssistant(data) {
    const article = appendMessage("助手", data.answer, "assistant");
    const content = article.querySelector(".message-content");

    if (data.sources && data.sources.length) {
        const sourceGrid = document.createElement("div");
        sourceGrid.className = "source-grid";
        data.sources.forEach((source) => {
            const card = document.createElement("div");
            card.className = "source-card";
            card.innerHTML = `
                <strong>${escapeHtml(source.name)}</strong>
                <span>${escapeHtml(source.category)} · 匹配分 ${source.score}</span>
                <p>${escapeHtml(source.excerpt)}</p>
            `;
            sourceGrid.appendChild(card);
        });
        content.appendChild(sourceGrid);
    }

    if (data.followUpQuestions && data.followUpQuestions.length) {
        const followUps = document.createElement("div");
        followUps.className = "follow-ups";
        data.followUpQuestions.forEach((question) => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = question;
            button.addEventListener("click", () => {
                input.value = question;
                input.focus();
            });
            followUps.appendChild(button);
        });
        content.appendChild(followUps);
    }
}

function appendMessage(role, text, type) {
    const template = document.querySelector("#messageTemplate");
    const article = template.content.firstElementChild.cloneNode(true);
    article.classList.add(type);
    article.querySelector(".message-role").textContent = role;
    article.querySelector(".message-content").textContent = text;
    chat.appendChild(article);
    chat.scrollTop = chat.scrollHeight;
    return article;
}

function setBusy(busy) {
    state.busy = busy;
    sendButton.disabled = busy;
    sendButton.textContent = busy ? "检索中" : "发送";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

init();
