const state = {
  scenarios: [],
  selectedScenarioId: null,
};

const elements = {
  scenarioList: document.getElementById("scenario-list"),
  scenarioTemplate: document.getElementById("scenario-item-template"),
  alternativeTemplate: document.getElementById("alternative-item-template"),
  overviewTemplate: document.getElementById("overview-item-template"),
  selectedScenarioTitle: document.getElementById("selected-scenario-title"),
  selectedScenarioExpected: document.getElementById("selected-scenario-expected"),
  selectedScenarioSeedCount: document.getElementById("selected-scenario-seed-count"),
  selectedScenarioDescription: document.getElementById("selected-scenario-description"),
  recommendationForm: document.getElementById("recommendation-form"),
  spendingMonth: document.getElementById("spending-month"),
  merchantName: document.getElementById("merchant-name"),
  merchantCategory: document.getElementById("merchant-category"),
  amount: document.getElementById("amount"),
  paymentTags: document.getElementById("payment-tags"),
  resetScenario: document.getElementById("reset-scenario"),
  resultState: document.getElementById("result-state"),
  recommendationResult: document.getElementById("recommendation-result"),
  recommendedCardName: document.getElementById("recommended-card-name"),
  recommendedCardId: document.getElementById("recommended-card-id"),
  recommendationReason: document.getElementById("recommendation-reason"),
  recommendationVerdict: document.getElementById("recommendation-verdict"),
  alternativeList: document.getElementById("alternative-list"),
  overviewGrid: document.getElementById("overview-grid"),
  lastRunAt: document.getElementById("last-run-at"),
};

document.addEventListener("DOMContentLoaded", () => {
  void bootstrap();
});

async function bootstrap() {
  try {
    setResultState("시나리오 목록을 불러오는 중입니다.", "loading");
    const response = await fetch("/api/demo-scenarios/recommendations");
    if (!response.ok) {
      throw new Error("데모 시나리오를 불러오지 못했습니다.");
    }

    state.scenarios = await response.json();
    renderScenarioList();

    if (state.scenarios.length === 0) {
      setResultState("표시할 데모 시나리오가 없습니다.", "error");
      return;
    }

    selectScenario(state.scenarios[0].id, true);
    bindEvents();
  } catch (error) {
    console.error(error);
    setResultState(error.message || "데모 화면을 초기화하지 못했습니다.", "error");
  }
}

function bindEvents() {
  elements.recommendationForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await runRecommendation();
  });

  elements.resetScenario.addEventListener("click", () => {
    const scenario = getSelectedScenario();
    if (!scenario) {
      return;
    }
    populateForm(scenario.request);
    void runRecommendation();
  });
}

function renderScenarioList() {
  elements.scenarioList.innerHTML = "";
  state.scenarios.forEach((scenario, index) => {
    const node = elements.scenarioTemplate.content.firstElementChild.cloneNode(true);
    node.dataset.id = scenario.id;
    node.querySelector(".scenario-tag").textContent = `Scenario ${String(index + 1).padStart(2, "0")}`;
    node.querySelector(".scenario-title").textContent = scenario.title;
    node.querySelector(".scenario-caption").textContent = scenario.description;
    node.addEventListener("click", () => selectScenario(scenario.id, true));
    elements.scenarioList.append(node);
  });
}

function selectScenario(scenarioId, autoRun = false) {
  state.selectedScenarioId = scenarioId;
  const scenario = getSelectedScenario();
  if (!scenario) {
    return;
  }

  elements.selectedScenarioTitle.textContent = scenario.title;
  elements.selectedScenarioExpected.textContent = scenario.expectedRecommendedCardId;
  elements.selectedScenarioSeedCount.textContent = String(scenario.seedRecordCount);
  elements.selectedScenarioDescription.textContent = scenario.description;
  populateForm(scenario.request);

  [...elements.scenarioList.children].forEach((child) => {
    child.classList.toggle("active", child.dataset.id === scenarioId);
  });

  if (autoRun) {
    void runRecommendation();
  }
}

function populateForm(request) {
  elements.spendingMonth.value = request.spendingMonth;
  elements.merchantName.value = request.merchantName;
  elements.merchantCategory.value = request.merchantCategory ?? "";
  elements.amount.value = String(request.amount);
  elements.paymentTags.value = request.paymentTags.join(", ");
}

async function runRecommendation() {
  const payload = buildPayloadFromForm();
  const scenario = getSelectedScenario();

  try {
    setResultState("추천 결과와 실적 현황을 계산하는 중입니다.", "loading");

    const [recommendationResponse, overviewResponse] = await Promise.all([
      fetch("/api/recommendations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }),
      fetch(`/api/performance-overview?yearMonth=${encodeURIComponent(payload.spendingMonth)}`),
    ]);

    if (!recommendationResponse.ok) {
      throw new Error("추천 결과를 가져오지 못했습니다.");
    }
    if (!overviewResponse.ok) {
      throw new Error("실적 현황을 가져오지 못했습니다.");
    }

    const recommendation = await recommendationResponse.json();
    const overview = await overviewResponse.json();

    renderRecommendation(recommendation, scenario?.expectedRecommendedCardId ?? null);
    renderOverview(overview);
    setResultState("추천 계산이 완료되었습니다.", "success");
  } catch (error) {
    console.error(error);
    hideRecommendationResult();
    elements.overviewGrid.innerHTML = "";
    setResultState(error.message || "추천 계산에 실패했습니다.", "error");
  }
}

function renderRecommendation(recommendation, expectedCardId) {
  elements.recommendationResult.classList.remove("hidden");
  elements.recommendedCardName.textContent = recommendation.recommendedCardName;
  elements.recommendedCardId.textContent = recommendation.recommendedCardId;
  elements.recommendationReason.textContent = recommendation.reason;
  elements.lastRunAt.textContent = `${new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date())} 실행`;

  const isMatch = expectedCardId && expectedCardId === recommendation.recommendedCardId;
  elements.recommendationVerdict.textContent = isMatch ? "Expected Match" : "Check Result";
  elements.recommendationVerdict.classList.remove("match", "mismatch");
  elements.recommendationVerdict.classList.add(isMatch ? "match" : "mismatch");

  elements.alternativeList.innerHTML = "";
  recommendation.alternatives.forEach((alternative) => {
    const node = elements.alternativeTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector(".alternative-name").textContent = alternative.cardName;
    node.querySelector(".alternative-score").textContent = `score ${alternative.score}`;
    node.querySelector(".alternative-reason").textContent = alternative.reason;
    elements.alternativeList.append(node);
  });
}

function renderOverview(overview) {
  elements.overviewGrid.innerHTML = "";
  overview.forEach((snapshot) => {
    const node = elements.overviewTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector(".overview-name").textContent = snapshot.cardName;
    node.querySelector(".overview-tier").textContent = snapshot.achieved
      ? `현재 최고 구간 달성 (${snapshot.targetTierCode})`
      : `다음 목표 ${snapshot.targetTierCode}`;
    node.querySelector(".overview-priority").textContent = `Priority ${snapshot.priority}`;
    node.querySelector(".spent-amount").textContent = formatWon(snapshot.spentAmount);
    node.querySelector(".target-amount").textContent = formatWon(snapshot.targetAmount);
    node.querySelector(".remaining-amount").textContent = snapshot.achieved
      ? "달성 완료"
      : formatWon(snapshot.remainingAmount);

    const percentage = snapshot.targetAmount === 0
      ? 100
      : Math.min(100, Math.round((snapshot.spentAmount / snapshot.targetAmount) * 100));
    node.querySelector(".progress-fill").style.width = `${percentage}%`;
    elements.overviewGrid.append(node);
  });
}

function setResultState(message, type) {
  elements.resultState.textContent = message;
  elements.resultState.className = `result-state ${type}`;
}

function hideRecommendationResult() {
  elements.recommendationResult.classList.add("hidden");
}

function buildPayloadFromForm() {
  return {
    spendingMonth: elements.spendingMonth.value,
    merchantName: elements.merchantName.value.trim(),
    merchantCategory: elements.merchantCategory.value.trim() || null,
    amount: Number(elements.amount.value),
    paymentTags: parseTags(elements.paymentTags.value),
  };
}

function parseTags(rawValue) {
  if (!rawValue.trim()) {
    return [];
  }
  return rawValue
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function getSelectedScenario() {
  return state.scenarios.find((scenario) => scenario.id === state.selectedScenarioId) ?? null;
}

function formatWon(amount) {
  return `${Number(amount).toLocaleString("ko-KR")}원`;
}
