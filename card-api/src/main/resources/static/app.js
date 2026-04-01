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
  tierFormList: document.getElementById("tier-form-list"),
  ruleFormList: document.getElementById("rule-form-list"),
  addTierButton: document.getElementById("add-tier"),
  addRuleButton: document.getElementById("add-rule"),
  patchPolicyTiers: document.getElementById("patch-policy-tiers"),
  patchPolicyRules: document.getElementById("patch-policy-rules"),
  replacePolicy: document.getElementById("replace-policy"),
  syncStatus: document.getElementById("sync-status"),
  syncTransactions: document.getElementById("sync-transactions"),
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

  elements.addTierButton.addEventListener("click", () => {
    elements.tierFormList.append(createTierRow());
  });

  elements.addRuleButton.addEventListener("click", () => {
    elements.ruleFormList.append(createRuleCard({}, true));
  });

  elements.syncTransactions.addEventListener("click", async () => {
    await syncTransactions();
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

async function syncTransactions() {
  const yearMonth = elements.spendingMonth.value;
  if (!yearMonth) {
    setSyncStatus("기준 월을 먼저 선택해 주세요.", "error");
    return;
  }

  try {
    setSyncStatus(`${yearMonth} 거래 내역을 카드사 API에서 동기화하는 중입니다.`, "loading");
    elements.syncTransactions.disabled = true;

    const result = await requestJson(
      "/api/sync/transactions",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ yearMonth }),
      },
      "카드사 거래 동기화에 실패했습니다.",
    );

    await refreshOverviewForCurrentMonth(false);
    setSyncStatus(
      `${result.syncedCardIds.length}개 카드에서 ${result.fetchedCount}건의 거래를 동기화했습니다.`,
      "success",
    );
  } catch (error) {
    console.error(error);
    setSyncStatus(error.message || "카드사 거래 동기화에 실패했습니다.", "error");
  } finally {
    elements.syncTransactions.disabled = false;
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
          tiers: readTiersFromForm(),
          benefitRules: readRulesFromForm(),
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
      ? { tiers: readTiersFromForm() }
      : { benefitRules: readRulesFromForm() };
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

  elements.tierFormList.innerHTML = "";
  if (policy.tiers.length === 0) {
    elements.tierFormList.innerHTML = '<div class="tier-empty">실적 구간이 없습니다. + 구간 추가 버튼으로 추가하세요.</div>';
  } else {
    policy.tiers.forEach((tier) => {
      elements.tierFormList.append(createTierRow(tier));
    });
  }

  elements.ruleFormList.innerHTML = "";
  if (policy.benefitRules.length === 0) {
    elements.ruleFormList.innerHTML = '<div class="rule-empty">혜택 규칙이 없습니다. + 규칙 추가 버튼으로 추가하세요.</div>';
  } else {
    policy.benefitRules.forEach((rule) => {
      elements.ruleFormList.append(createRuleCard(rule, false));
    });
  }
}

function clearPolicyEditor() {
  elements.policyTierCount.textContent = "-";
  elements.policyRuleCount.textContent = "-";
  elements.tierFormList.innerHTML = "";
  elements.ruleFormList.innerHTML = "";
}

function setPolicyEditorEnabled(enabled) {
  elements.loadPolicy.disabled = !enabled;
  elements.addTierButton.disabled = !enabled;
  elements.addRuleButton.disabled = !enabled;
  elements.patchPolicyTiers.disabled = !enabled;
  elements.patchPolicyRules.disabled = !enabled;
  elements.replacePolicy.disabled = !enabled;
}

function createTierRow(tier = {}) {
  const row = document.createElement("div");
  row.className = "tier-row";

  const codeInput = document.createElement("input");
  codeInput.type = "text";
  codeInput.className = "tier-code";
  codeInput.placeholder = "구간 코드";
  codeInput.value = tier.code || "";

  const amountInput = document.createElement("input");
  amountInput.type = "number";
  amountInput.className = "tier-amount";
  amountInput.placeholder = "목표 금액";
  amountInput.min = "0";
  amountInput.step = "10000";
  amountInput.value = tier.targetAmount ?? 0;

  const summaryInput = document.createElement("input");
  summaryInput.type = "text";
  summaryInput.className = "tier-summary";
  summaryInput.placeholder = "혜택 요약";
  summaryInput.value = tier.benefitSummary || "";

  const removeButton = document.createElement("button");
  removeButton.type = "button";
  removeButton.className = "ghost-button";
  removeButton.textContent = "삭제";
  removeButton.addEventListener("click", () => {
    row.remove();
    if (elements.tierFormList.children.length === 0) {
      elements.tierFormList.innerHTML = '<div class="tier-empty">실적 구간이 없습니다. + 구간 추가 버튼으로 추가하세요.</div>';
    }
  });

  row.append(codeInput, amountInput, summaryInput, removeButton);

  const emptyMsg = elements.tierFormList.querySelector(".tier-empty");
  if (emptyMsg) emptyMsg.remove();

  return row;
}

function createRuleCard(rule = {}, expanded = false) {
  const card = document.createElement("div");
  card.className = "rule-card";

  const isPercent = (rule.benefitType || "RATE_PERCENT") === "RATE_PERCENT";

  const header = document.createElement("div");
  header.className = "rule-header";

  const headerInfo = document.createElement("div");
  headerInfo.className = "rule-header-info";

  const headerId = document.createElement("strong");
  headerId.className = "rule-header-id";
  headerId.textContent = rule.ruleId || "새 규칙";

  const headerType = document.createElement("span");
  headerType.className = "rule-header-type";
  headerType.textContent = rule.benefitType || "RATE_PERCENT";

  const headerSummary = document.createElement("span");
  headerSummary.className = "rule-header-summary";
  headerSummary.textContent = rule.benefitSummary || "";

  headerInfo.append(headerId, headerType, headerSummary);

  const headerActions = document.createElement("div");
  headerActions.className = "rule-header-actions";

  const toggleBtn = document.createElement("button");
  toggleBtn.type = "button";
  toggleBtn.className = "ghost-button";
  toggleBtn.textContent = expanded ? "접기" : "펼치기";

  const removeBtn = document.createElement("button");
  removeBtn.type = "button";
  removeBtn.className = "ghost-button";
  removeBtn.textContent = "삭제";

  headerActions.append(toggleBtn, removeBtn);
  header.append(headerInfo, headerActions);

  const body = document.createElement("div");
  body.className = "rule-body" + (expanded ? "" : " hidden");

  body.innerHTML = buildRuleBodyHtml(rule, isPercent);

  card.append(header, body);

  // Set values via DOM to avoid XSS
  setRuleFieldValues(body, rule, isPercent);

  // Event: toggle
  const toggleBody = () => {
    const isHidden = body.classList.toggle("hidden");
    toggleBtn.textContent = isHidden ? "펼치기" : "접기";
  };
  header.addEventListener("click", (e) => {
    if (e.target.closest("button")) return;
    toggleBody();
  });
  toggleBtn.addEventListener("click", toggleBody);

  // Event: remove
  removeBtn.addEventListener("click", () => {
    card.remove();
    if (elements.ruleFormList.children.length === 0) {
      elements.ruleFormList.innerHTML = '<div class="rule-empty">혜택 규칙이 없습니다. + 규칙 추가 버튼으로 추가하세요.</div>';
    }
  });

  // Event: type toggle
  const typeSelect = body.querySelector(".rule-type");
  typeSelect.addEventListener("change", () => {
    const isRate = typeSelect.value === "RATE_PERCENT";
    body.querySelector(".rate-field").classList.toggle("hidden", !isRate);
    body.querySelector(".fixed-field").classList.toggle("hidden", isRate);
    headerType.textContent = typeSelect.value;
  });

  // Event: sync header
  const ruleIdInput = body.querySelector(".rule-id");
  ruleIdInput.addEventListener("input", () => {
    headerId.textContent = ruleIdInput.value || "새 규칙";
  });
  body.querySelector(".rule-summary").addEventListener("input", (e) => {
    headerSummary.textContent = e.target.value;
  });

  // Monthly cap tiers
  const monthlyCapList = body.querySelector(".monthly-cap-list");
  body.querySelector(".add-cap-tier").addEventListener("click", () => {
    monthlyCapList.append(createCapTierRow());
  });
  (rule.monthlyCapTiers || []).forEach((ct) => {
    monthlyCapList.append(createCapTierRow(ct));
  });

  // Shared monthly cap tiers
  const sharedCapList = body.querySelector(".shared-cap-list");
  body.querySelector(".add-shared-cap-tier").addEventListener("click", () => {
    sharedCapList.append(createCapTierRow());
  });
  (rule.sharedMonthlyCapTiers || []).forEach((ct) => {
    sharedCapList.append(createCapTierRow(ct));
  });

  const emptyMsg = elements.ruleFormList.querySelector(".rule-empty");
  if (emptyMsg) emptyMsg.remove();

  return card;
}

function buildRuleBodyHtml(rule, isPercent) {
  return `
    <fieldset class="rule-fieldset">
      <legend>기본 정보</legend>
      <div class="rule-fields">
        <label><span>규칙 ID</span><input type="text" class="rule-id" placeholder="고유 식별자"></label>
        <label class="wide"><span>혜택 요약</span><input type="text" class="rule-summary" placeholder="혜택 설명"></label>
        <label><span>혜택 유형</span>
          <select class="rule-type">
            <option value="RATE_PERCENT"${isPercent ? " selected" : ""}>비율 할인 (RATE_PERCENT)</option>
            <option value="FIXED_AMOUNT"${!isPercent ? " selected" : ""}>정액 할인 (FIXED_AMOUNT)</option>
          </select>
        </label>
      </div>
    </fieldset>
    <fieldset class="rule-fieldset">
      <legend>혜택 금액</legend>
      <div class="rule-fields">
        <label class="rate-field${isPercent ? "" : " hidden"}"><span>할인율 (basis points, 100 = 1%)</span><input type="number" class="rule-rate" min="0" max="10000"></label>
        <label class="fixed-field${!isPercent ? "" : " hidden"}"><span>정액 할인 (원)</span><input type="number" class="rule-fixed" min="0"></label>
      </div>
    </fieldset>
    <fieldset class="rule-fieldset">
      <legend>매칭 조건</legend>
      <div class="rule-fields">
        <label><span>가맹점 카테고리</span><input type="text" class="rule-categories" placeholder="쉼표 구분 (비우면 ANY)"></label>
        <label><span>가맹점 키워드</span><input type="text" class="rule-keywords" placeholder="쉼표 구분"></label>
        <label><span>필수 태그</span><input type="text" class="rule-required-tags" placeholder="쉼표 구분"></label>
        <label><span>제외 태그</span><input type="text" class="rule-excluded-tags" placeholder="쉼표 구분"></label>
      </div>
    </fieldset>
    <fieldset class="rule-fieldset">
      <legend>자격 조건</legend>
      <div class="rule-fields">
        <label><span>최소 결제 금액</span><input type="number" class="rule-min-payment" min="0"></label>
        <label><span>건당 혜택 한도</span><input type="number" class="rule-per-tx-cap" min="0"></label>
        <label><span>전월 최소 실적</span><input type="number" class="rule-min-prev-month" min="0"></label>
      </div>
    </fieldset>
    <fieldset class="rule-fieldset">
      <legend>한도 설정</legend>
      <div class="rule-fields">
        <label><span>월간 혜택 횟수 (0=무제한)</span><input type="number" class="rule-monthly-count" min="0"></label>
        <label><span>연간 혜택 한도 (원)</span><input type="number" class="rule-yearly-cap" min="0"></label>
        <label><span>연간 혜택 횟수 (0=무제한)</span><input type="number" class="rule-yearly-count" min="0"></label>
      </div>
      <div class="cap-tiers-section">
        <div class="subsection-header">
          <span class="field-hint">월간 혜택 한도 (전월 실적별)</span>
          <button type="button" class="ghost-button add-cap-tier">+ 구간</button>
        </div>
        <div class="cap-tier-list monthly-cap-list"></div>
      </div>
    </fieldset>
    <fieldset class="rule-fieldset">
      <legend>그룹 설정</legend>
      <div class="rule-fields">
        <label><span>배타적 그룹 ID</span><input type="text" class="rule-exclusive-group" placeholder="비우면 ruleId 사용"></label>
        <label><span>공유 한도 그룹 ID</span><input type="text" class="rule-shared-group" placeholder="비우면 그룹 없음"></label>
        <label><span>공유 연간 한도 (원)</span><input type="number" class="rule-shared-yearly-cap" min="0"></label>
      </div>
      <div class="cap-tiers-section">
        <div class="subsection-header">
          <span class="field-hint">공유 월간 한도 (전월 실적별)</span>
          <button type="button" class="ghost-button add-shared-cap-tier">+ 구간</button>
        </div>
        <div class="cap-tier-list shared-cap-list"></div>
      </div>
    </fieldset>`;
}

function setRuleFieldValues(body, rule, isPercent) {
  body.querySelector(".rule-id").value = rule.ruleId || "";
  body.querySelector(".rule-summary").value = rule.benefitSummary || "";
  body.querySelector(".rule-rate").value = rule.rateBasisPoints ?? 0;
  body.querySelector(".rule-fixed").value = rule.fixedBenefitAmount ?? 0;
  body.querySelector(".rule-categories").value = (rule.merchantCategories || []).join(", ");
  body.querySelector(".rule-keywords").value = (rule.merchantKeywords || []).join(", ");
  body.querySelector(".rule-required-tags").value = (rule.requiredTags || []).join(", ");
  body.querySelector(".rule-excluded-tags").value = (rule.excludedTags || []).join(", ");
  body.querySelector(".rule-min-payment").value = rule.minimumPaymentAmount ?? 0;
  body.querySelector(".rule-per-tx-cap").value = rule.perTransactionCap ?? 0;
  body.querySelector(".rule-min-prev-month").value = rule.minimumPreviousMonthSpent ?? 0;
  body.querySelector(".rule-monthly-count").value = rule.monthlyCountLimit ?? 0;
  body.querySelector(".rule-yearly-cap").value = rule.yearlyBenefitCap ?? 0;
  body.querySelector(".rule-yearly-count").value = rule.yearlyCountLimit ?? 0;
  body.querySelector(".rule-exclusive-group").value = rule.exclusiveGroupId || "";
  body.querySelector(".rule-shared-group").value = rule.sharedLimitGroupId || "";
  body.querySelector(".rule-shared-yearly-cap").value = rule.sharedYearlyBenefitCap ?? 0;
}

function createCapTierRow(capTier = {}) {
  const row = document.createElement("div");
  row.className = "cap-tier-row";

  const minInput = document.createElement("input");
  minInput.type = "number";
  minInput.className = "cap-min-spent";
  minInput.placeholder = "전월 최소 실적";
  minInput.min = "0";
  minInput.value = capTier.minimumPreviousMonthSpent ?? 0;

  const capInput = document.createElement("input");
  capInput.type = "number";
  capInput.className = "cap-amount";
  capInput.placeholder = "월간 한도";
  capInput.min = "0";
  capInput.value = capTier.monthlyCap ?? 0;

  const removeButton = document.createElement("button");
  removeButton.type = "button";
  removeButton.className = "ghost-button";
  removeButton.textContent = "삭제";
  removeButton.addEventListener("click", () => row.remove());

  row.append(minInput, capInput, removeButton);
  return row;
}

function readTiersFromForm() {
  return Array.from(elements.tierFormList.querySelectorAll(".tier-row")).map((row) => ({
    code: row.querySelector(".tier-code").value.trim(),
    targetAmount: Number(row.querySelector(".tier-amount").value) || 0,
    benefitSummary: row.querySelector(".tier-summary").value.trim(),
  }));
}

function readRulesFromForm() {
  return Array.from(elements.ruleFormList.querySelectorAll(".rule-card")).map((card) => {
    const body = card.querySelector(".rule-body");
    const type = body.querySelector(".rule-type").value;

    const rule = {
      ruleId: body.querySelector(".rule-id").value.trim(),
      benefitSummary: body.querySelector(".rule-summary").value.trim(),
      benefitType: type,
      merchantCategories: parseTags(body.querySelector(".rule-categories").value),
      merchantKeywords: parseTags(body.querySelector(".rule-keywords").value),
      requiredTags: parseTags(body.querySelector(".rule-required-tags").value),
      excludedTags: parseTags(body.querySelector(".rule-excluded-tags").value),
      minimumPaymentAmount: Number(body.querySelector(".rule-min-payment").value) || 0,
      perTransactionCap: Number(body.querySelector(".rule-per-tx-cap").value) || 0,
      minimumPreviousMonthSpent: Number(body.querySelector(".rule-min-prev-month").value) || 0,
      monthlyCountLimit: Number(body.querySelector(".rule-monthly-count").value) || 0,
      yearlyBenefitCap: Number(body.querySelector(".rule-yearly-cap").value) || 0,
      yearlyCountLimit: Number(body.querySelector(".rule-yearly-count").value) || 0,
      exclusiveGroupId: body.querySelector(".rule-exclusive-group").value.trim() || null,
      sharedLimitGroupId: body.querySelector(".rule-shared-group").value.trim() || null,
      sharedYearlyBenefitCap: Number(body.querySelector(".rule-shared-yearly-cap").value) || 0,
    };

    if (type === "RATE_PERCENT") {
      rule.rateBasisPoints = Number(body.querySelector(".rule-rate").value) || 0;
    } else {
      rule.fixedBenefitAmount = Number(body.querySelector(".rule-fixed").value) || 0;
    }

    const monthlyCapRows = body.querySelectorAll(".monthly-cap-list .cap-tier-row");
    if (monthlyCapRows.length > 0) {
      rule.monthlyCapTiers = Array.from(monthlyCapRows).map((row) => ({
        minimumPreviousMonthSpent: Number(row.querySelector(".cap-min-spent").value) || 0,
        monthlyCap: Number(row.querySelector(".cap-amount").value) || 0,
      }));
    }

    const sharedCapRows = body.querySelectorAll(".shared-cap-list .cap-tier-row");
    if (sharedCapRows.length > 0) {
      rule.sharedMonthlyCapTiers = Array.from(sharedCapRows).map((row) => ({
        minimumPreviousMonthSpent: Number(row.querySelector(".cap-min-spent").value) || 0,
        monthlyCap: Number(row.querySelector(".cap-amount").value) || 0,
      }));
    }

    return rule;
  });
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

function setSyncStatus(message, type) {
  elements.syncStatus.textContent = message;
  elements.syncStatus.className = `inline-status ${type}`;
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


function getSelectedScenario() {
  return state.scenarios.find((scenario) => scenario.id === state.selectedScenarioId) ?? null;
}

function formatWon(amount) {
  return `${Number(amount).toLocaleString("ko-KR")}원`;
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
