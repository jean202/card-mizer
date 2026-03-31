const state = {
  scenarios: [],
  selectedScenarioId: null,
  overview: [],
  priorityDraftIds: [],
  selectedPolicyCardId: null,
  currentPolicy: null,
};

const elements = {
  scenarioList: document.getElementById("scenario-list"),
  scenarioTemplate: document.getElementById("scenario-item-template"),
  alternativeTemplate: document.getElementById("alternative-item-template"),
  overviewTemplate: document.getElementById("overview-item-template"),
  priorityTemplate: document.getElementById("priority-item-template"),
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
  catalogStatus: document.getElementById("catalog-status"),
  catalogCount: document.getElementById("catalog-count"),
  registerCardForm: document.getElementById("register-card-form"),
  registerCardId: document.getElementById("register-card-id"),
  registerIssuerName: document.getElementById("register-issuer-name"),
  registerProductName: document.getElementById("register-product-name"),
  registerCardType: document.getElementById("register-card-type"),
  registerPriority: document.getElementById("register-priority"),
  resetRegisterForm: document.getElementById("reset-register-form"),
  priorityList: document.getElementById("priority-list"),
  applyPriorities: document.getElementById("apply-priorities"),
  refreshCatalog: document.getElementById("refresh-catalog"),
  policyStatus: document.getElementById("policy-status"),
  policyCardSelect: document.getElementById("policy-card-select"),
  loadPolicy: document.getElementById("load-policy"),
  policyTierCount: document.getElementById("policy-tier-count"),
  policyRuleCount: document.getElementById("policy-rule-count"),
  policyTiersEditor: document.getElementById("policy-tiers-editor"),
  policyRulesEditor: document.getElementById("policy-rules-editor"),
  patchPolicyTiers: document.getElementById("patch-policy-tiers"),
  patchPolicyRules: document.getElementById("patch-policy-rules"),
  replacePolicy: document.getElementById("replace-policy"),
};

document.addEventListener("DOMContentLoaded", () => {
  void bootstrap();
});

async function bootstrap() {
  bindEvents();
  resetRegisterForm();
  renderPriorityList();
  setPolicyEditorEnabled(false);

  try {
    setResultState("시나리오 목록을 불러오는 중입니다.", "loading");
    const scenarios = await requestJson(
      "/api/demo-scenarios/recommendations",
      {},
      "데모 시나리오를 불러오지 못했습니다.",
    );

    state.scenarios = scenarios;
    renderScenarioList();

    if (state.scenarios.length === 0) {
      setResultState("표시할 데모 시나리오가 없습니다.", "error");
      setCatalogStatus("카드 목록이 비어 있습니다.", "error");
      setPolicyStatus("편집할 카드가 없습니다.", "error");
      return;
    }

    selectScenario(state.scenarios[0].id, true);
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

  elements.spendingMonth.addEventListener("change", () => {
    void refreshOverviewForCurrentMonth(false);
  });

  elements.registerCardForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await registerCard();
  });

  elements.resetRegisterForm.addEventListener("click", () => {
    resetRegisterForm();
  });

  elements.applyPriorities.addEventListener("click", async () => {
    await updatePriorities();
  });

  elements.refreshCatalog.addEventListener("click", async () => {
    setCatalogStatus("카드 목록과 실적 현황을 새로고침하는 중입니다.", "loading");
    try {
      await refreshOverviewForCurrentMonth(false);
      setCatalogStatus("카드 목록을 새로고침했습니다.", "success");
    } catch (error) {
      console.error(error);
      setCatalogStatus(error.message || "카드 목록을 새로고침하지 못했습니다.", "error");
    }
  });

  elements.policyCardSelect.addEventListener("change", () => {
    state.selectedPolicyCardId = elements.policyCardSelect.value || null;
    void loadSelectedPolicy();
  });

  elements.loadPolicy.addEventListener("click", async () => {
    await loadSelectedPolicy();
  });

  elements.replacePolicy.addEventListener("click", async () => {
    await replacePolicy();
  });

  elements.patchPolicyTiers.addEventListener("click", async () => {
    await patchPolicy("tiers");
  });

  elements.patchPolicyRules.addEventListener("click", async () => {
    await patchPolicy("benefitRules");
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

    const [recommendation, overview] = await Promise.all([
      requestJson(
        "/api/recommendations",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        },
        "추천 결과를 가져오지 못했습니다.",
      ),
      requestJson(
        `/api/performance-overview?yearMonth=${encodeURIComponent(payload.spendingMonth)}`,
        {},
        "실적 현황을 가져오지 못했습니다.",
      ),
    ]);

    renderRecommendation(recommendation, scenario?.expectedRecommendedCardId ?? null);
    renderOverview(overview);
    setResultState("추천 계산이 완료되었습니다.", "success");
  } catch (error) {
    console.error(error);
    hideRecommendationResult();
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
  state.overview = [...overview].sort((left, right) => left.priority - right.priority);
  state.priorityDraftIds = state.overview.map((snapshot) => snapshot.cardId);

  elements.overviewGrid.innerHTML = "";
  if (state.overview.length === 0) {
    elements.overviewGrid.innerHTML = '<div class="empty-state">표시할 카드 실적 현황이 없습니다.</div>';
  }

  state.overview.forEach((snapshot) => {
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

  syncCatalogViews();
}

function syncCatalogViews() {
  elements.catalogCount.textContent = `${state.overview.length} cards`;
  renderPriorityList();
  syncPolicyCardOptions();
  updateRegisterPriorityDefault();
  setCatalogStatus("실적 현황 기준 카드 목록이 동기화되었습니다.", "success");
}

function renderPriorityList() {
  elements.priorityList.innerHTML = "";

  if (state.priorityDraftIds.length === 0) {
    elements.priorityList.innerHTML = '<div class="empty-state">카드가 없어서 우선순위를 정렬할 수 없습니다.</div>';
    elements.applyPriorities.disabled = true;
    return;
  }

  elements.applyPriorities.disabled = false;
  state.priorityDraftIds.forEach((cardId, index) => {
    const snapshot = state.overview.find((item) => item.cardId === cardId);
    if (!snapshot) {
      return;
    }

    const node = elements.priorityTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector(".priority-rank").textContent = String(index + 1).padStart(2, "0");
    node.querySelector(".priority-name").textContent = snapshot.cardName;
    node.querySelector(".priority-id").textContent = snapshot.cardId;

    const moveUpButton = node.querySelector(".move-up");
    const moveDownButton = node.querySelector(".move-down");
    moveUpButton.disabled = index === 0;
    moveDownButton.disabled = index === state.priorityDraftIds.length - 1;

    moveUpButton.addEventListener("click", () => {
      movePriority(cardId, -1);
    });
    moveDownButton.addEventListener("click", () => {
      movePriority(cardId, 1);
    });

    elements.priorityList.append(node);
  });
}

function movePriority(cardId, direction) {
  const currentIndex = state.priorityDraftIds.indexOf(cardId);
  const nextIndex = currentIndex + direction;
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= state.priorityDraftIds.length) {
    return;
  }

  const reorderedIds = [...state.priorityDraftIds];
  [reorderedIds[currentIndex], reorderedIds[nextIndex]] = [reorderedIds[nextIndex], reorderedIds[currentIndex]];
  state.priorityDraftIds = reorderedIds;
  renderPriorityList();
  setCatalogStatus("저장 전 우선순위 순서를 수정했습니다.", "loading");
}

async function registerCard() {
  const payload = {
    cardId: elements.registerCardId.value.trim(),
    issuerName: elements.registerIssuerName.value.trim(),
    productName: elements.registerProductName.value.trim(),
    cardType: elements.registerCardType.value,
    priority: Number(elements.registerPriority.value),
  };

  try {
    setCatalogStatus("새 카드를 등록하는 중입니다.", "loading");
    const response = await requestJson(
      "/api/cards",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      },
      "카드를 등록하지 못했습니다.",
    );

    await refreshOverviewForCurrentMonth(true);
    resetRegisterForm();
    selectPolicyCard(response.cardId);
    await loadSelectedPolicy();
    setCatalogStatus(`${response.cardName} 카드를 등록했습니다.`, "success");
  } catch (error) {
    console.error(error);
    setCatalogStatus(error.message || "카드를 등록하지 못했습니다.", "error");
  }
}

async function updatePriorities() {
  if (state.priorityDraftIds.length === 0) {
    return;
  }

  try {
    setCatalogStatus("카드 우선순위를 저장하는 중입니다.", "loading");
    await requestVoid(
      "/api/cards/priorities",
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ orderedCardIds: state.priorityDraftIds }),
      },
      "카드 우선순위를 저장하지 못했습니다.",
    );

    await refreshOverviewForCurrentMonth(true);
    setCatalogStatus("카드 우선순위를 저장했습니다.", "success");
  } catch (error) {
    console.error(error);
    setCatalogStatus(error.message || "카드 우선순위를 저장하지 못했습니다.", "error");
  }
}

async function refreshOverviewForCurrentMonth(showCatalogSuccessMessage) {
  const yearMonth = elements.spendingMonth.value;
  if (!yearMonth) {
    return [];
  }

  const overview = await requestJson(
    `/api/performance-overview?yearMonth=${encodeURIComponent(yearMonth)}`,
    {},
    "실적 현황을 가져오지 못했습니다.",
  );
  renderOverview(overview);

  if (showCatalogSuccessMessage) {
    setCatalogStatus("카드 목록과 실적 현황을 새로고침했습니다.", "success");
  }

  return overview;
}

function syncPolicyCardOptions() {
  const previousCardId = state.selectedPolicyCardId;
  elements.policyCardSelect.innerHTML = "";

  if (state.overview.length === 0) {
    state.selectedPolicyCardId = null;
    state.currentPolicy = null;
    elements.policyCardSelect.disabled = true;
    elements.policyCardSelect.innerHTML = '<option value="">카드 없음</option>';
    clearPolicyEditor();
    setPolicyEditorEnabled(false);
    setPolicyStatus("편집할 카드가 없습니다.", "error");
    return;
  }

  state.overview.forEach((snapshot) => {
    const option = document.createElement("option");
    option.value = snapshot.cardId;
    option.textContent = `${snapshot.priority}. ${snapshot.cardName}`;
    elements.policyCardSelect.append(option);
  });

  elements.policyCardSelect.disabled = false;
  setPolicyEditorEnabled(true);

  if (state.overview.some((snapshot) => snapshot.cardId === previousCardId)) {
    state.selectedPolicyCardId = previousCardId;
  } else {
    state.selectedPolicyCardId = state.overview[0].cardId;
  }

  elements.policyCardSelect.value = state.selectedPolicyCardId;

  if (!state.currentPolicy || state.currentPolicy.cardId !== state.selectedPolicyCardId) {
    void loadSelectedPolicy();
  }
}

function selectPolicyCard(cardId) {
  if (!state.overview.some((snapshot) => snapshot.cardId === cardId)) {
    return;
  }
  state.selectedPolicyCardId = cardId;
  elements.policyCardSelect.value = cardId;
}

async function loadSelectedPolicy() {
  if (!state.selectedPolicyCardId) {
    return;
  }

  try {
    setPolicyStatus(`${state.selectedPolicyCardId} 정책을 불러오는 중입니다.`, "loading");
    const policy = await requestJson(
      `/api/cards/${encodeURIComponent(state.selectedPolicyCardId)}/performance-policy`,
      {},
      "카드 정책을 불러오지 못했습니다.",
    );

    state.currentPolicy = policy;
    renderPolicyEditor(policy);
    setPolicyStatus(`${policy.cardId} 정책을 불러왔습니다.`, "success");
  } catch (error) {
    console.error(error);
    setPolicyStatus(error.message || "카드 정책을 불러오지 못했습니다.", "error");
  }
}

async function replacePolicy() {
  if (!state.selectedPolicyCardId) {
    return;
  }

  try {
    setPolicyStatus(`${state.selectedPolicyCardId} 정책을 전체 교체하는 중입니다.`, "loading");
    const response = await requestJson(
      `/api/cards/${encodeURIComponent(state.selectedPolicyCardId)}/performance-policy`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tiers: parseEditorArray(elements.policyTiersEditor.value, "tiers"),
          benefitRules: parseEditorArray(elements.policyRulesEditor.value, "benefitRules"),
        }),
      },
      "카드 정책을 저장하지 못했습니다.",
    );

    state.currentPolicy = response;
    renderPolicyEditor(response);
    await refreshOverviewForCurrentMonth(false);
    setPolicyStatus(`${response.cardId} 정책을 전체 교체했습니다.`, "success");
  } catch (error) {
    console.error(error);
    setPolicyStatus(error.message || "카드 정책을 저장하지 못했습니다.", "error");
  }
}

async function patchPolicy(fieldName) {
  if (!state.selectedPolicyCardId) {
    return;
  }

  try {
    const patchBody = fieldName === "tiers"
      ? { tiers: parseEditorArray(elements.policyTiersEditor.value, "tiers") }
      : { benefitRules: parseEditorArray(elements.policyRulesEditor.value, "benefitRules") };
    const successLabel = fieldName === "tiers" ? "실적 구간" : "혜택 규칙";

    setPolicyStatus(`${successLabel}만 PATCH 적용하는 중입니다.`, "loading");
    const response = await requestJson(
      `/api/cards/${encodeURIComponent(state.selectedPolicyCardId)}/performance-policy`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(patchBody),
      },
      "카드 정책 PATCH에 실패했습니다.",
    );

    state.currentPolicy = response;
    renderPolicyEditor(response);
    await refreshOverviewForCurrentMonth(false);
    setPolicyStatus(`${response.cardId} 정책의 ${successLabel}을(를) 업데이트했습니다.`, "success");
  } catch (error) {
    console.error(error);
    setPolicyStatus(error.message || "카드 정책 PATCH에 실패했습니다.", "error");
  }
}

function renderPolicyEditor(policy) {
  elements.policyTierCount.textContent = `${policy.tiers.length} tiers`;
  elements.policyRuleCount.textContent = `${policy.benefitRules.length} rules`;
  elements.policyTiersEditor.value = formatJson(policy.tiers);
  elements.policyRulesEditor.value = formatJson(policy.benefitRules);
}

function clearPolicyEditor() {
  elements.policyTierCount.textContent = "-";
  elements.policyRuleCount.textContent = "-";
  elements.policyTiersEditor.value = "[]";
  elements.policyRulesEditor.value = "[]";
}

function setPolicyEditorEnabled(enabled) {
  elements.loadPolicy.disabled = !enabled;
  elements.policyTiersEditor.disabled = !enabled;
  elements.policyRulesEditor.disabled = !enabled;
  elements.patchPolicyTiers.disabled = !enabled;
  elements.patchPolicyRules.disabled = !enabled;
  elements.replacePolicy.disabled = !enabled;
}

function resetRegisterForm() {
  elements.registerCardForm.reset();
  elements.registerCardType.value = "CREDIT";
  updateRegisterPriorityDefault();
}

function updateRegisterPriorityDefault() {
  elements.registerPriority.value = String(Math.max(1, state.overview.length + 1));
}

function setResultState(message, type) {
  elements.resultState.textContent = message;
  elements.resultState.className = `result-state ${type}`;
}

function setCatalogStatus(message, type) {
  elements.catalogStatus.textContent = message;
  elements.catalogStatus.className = `inline-status ${type}`;
}

function setPolicyStatus(message, type) {
  elements.policyStatus.textContent = message;
  elements.policyStatus.className = `inline-status ${type}`;
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

function parseEditorArray(rawValue, label) {
  try {
    const parsed = JSON.parse(rawValue);
    if (!Array.isArray(parsed)) {
      throw new Error(`${label} 편집 값은 JSON 배열이어야 합니다.`);
    }
    return parsed;
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error(`${label} 편집 값의 JSON 형식이 올바르지 않습니다.`);
    }
    throw error;
  }
}

function getSelectedScenario() {
  return state.scenarios.find((scenario) => scenario.id === state.selectedScenarioId) ?? null;
}

function formatWon(amount) {
  return `${Number(amount).toLocaleString("ko-KR")}원`;
}

function formatJson(value) {
  return JSON.stringify(value, null, 2);
}

async function requestJson(url, options = {}, fallbackMessage = "요청 처리에 실패했습니다.") {
  const response = await fetch(url, options);
  const body = await readResponseBody(response);
  if (!response.ok) {
    throw new Error(extractApiErrorMessage(body, fallbackMessage));
  }
  return body;
}

async function requestVoid(url, options = {}, fallbackMessage = "요청 처리에 실패했습니다.") {
  const response = await fetch(url, options);
  const body = await readResponseBody(response);
  if (!response.ok) {
    throw new Error(extractApiErrorMessage(body, fallbackMessage));
  }
}

async function readResponseBody(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();
  return text ? text : null;
}

function extractApiErrorMessage(body, fallbackMessage) {
  if (body && typeof body === "object") {
    const fieldErrors = Array.isArray(body.fieldErrors)
      ? body.fieldErrors
          .map((fieldError) => `${fieldError.field}: ${fieldError.message}`)
          .join(", ")
      : "";
    if (body.message && fieldErrors) {
      return `${body.message} (${fieldErrors})`;
    }
    if (body.message) {
      return body.message;
    }
  }

  if (typeof body === "string" && body.trim()) {
    return body;
  }

  return fallbackMessage;
}
