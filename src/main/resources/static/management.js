(() => {
  const api = "/api/v1";
  const views = {
    courseLevels: {
      title: "Course levels",
      description: "Configure the course levels offered by each institute.",
      path: "/course-levels",
      columns: [["Institute", "instituteName"], ["Code", "code"], ["Level", "name"]],
      fields: [
        { name: "instituteId", label: "Institute", required: true, type: "select", source: "institutes", value: "id", text: "name" },
        { name: "code", label: "Level code", required: true, maxLength: 30 },
        { name: "name", label: "Level name", required: true, maxLength: 100 }
      ]
    },
    courses: {
      title: "Courses",
      description: "Create courses and associate each with a course level.",
      path: "/courses",
      columns: [["Institute", "instituteName"], ["Code", "code"], ["Course", "name"], ["Course level", "levelName"]],
      fields: [
        { name: "code", label: "Course code", required: true, maxLength: 30 },
        { name: "name", label: "Course name", required: true, maxLength: 150 },
        { name: "levelId", label: "Course level", required: true, type: "select", source: "courseLevels", value: "id", text: "name" }
      ]
    },
    classes: {
      title: "Classes & scores",
      description: "Create course sections, enroll students, and manage assessments and scores.",
      path: "/classes",
      columns: [["Institute", "instituteName"], ["Course", "courseName"], ["Class / section", "name"], ["Academic year", "academicYear"]],
      fields: [
        { name: "courseId", label: "Course", required: true, type: "select", source: "courses", value: "id", text: "name" },
        { name: "name", label: "Class or section", required: true, maxLength: 100 },
        { name: "academicYear", label: "Academic year", required: true, maxLength: 20, placeholder: "2026-2027" }
      ],
      action: "Manage"
    },
    students: {
      title: "Students",
      description: "Maintain the student directory used for course enrollment and library membership.",
      path: "/students",
      columns: [["Institute", "instituteName"], ["Student number", "studentNumber"], ["Name", "name"], ["Email", "email"]],
      fields: [
        { name: "instituteId", label: "Institute", required: true, type: "select", source: "institutes", value: "id", text: "name" },
        { name: "studentNumber", label: "Student number", required: true, maxLength: 40 },
        { name: "name", label: "Student name", required: true, maxLength: 150 },
        { name: "email", label: "Email", type: "email", maxLength: 254 }
      ]
    },
    books: {
      title: "Library catalog",
      description: "Manage books and physical copy counts.",
      path: "/library/books",
      columns: [["Institute", "instituteName"], ["ISBN", "isbn"], ["Title", "title"], ["Author", "author"], ["Copies", "totalCopies"], ["Available", "availableCopies"]],
      fields: [
        { name: "instituteId", label: "Institute", required: true, type: "select", source: "institutes", value: "id", text: "name" },
        { name: "isbn", label: "ISBN", required: true, maxLength: 20 },
        { name: "title", label: "Book title", required: true, maxLength: 200 },
        { name: "author", label: "Author", required: true, maxLength: 150 },
        { name: "totalCopies", label: "Total copies", required: true, type: "number", min: 0, step: 1 }
      ]
    },
    loans: {
      title: "Library loans",
      description: "Check books out to institute students and record their returns.",
      path: "/library/loans",
      columns: [["Book", "bookTitle"], ["Student", "studentName"], ["Student number", "studentNumber"], ["Due date", "dueDate"], ["Status", "status"]],
      fields: [
        { name: "bookId", label: "Book", required: true, type: "select", source: "books", value: "id", text: "title" },
        { name: "studentId", label: "Student", required: true, type: "select", source: "students", value: "id", text: "name" },
        { name: "dueDate", label: "Due date", required: true, type: "date" }
      ],
      action: "Return"
    }
  };
  const cache = {};
  const mainContent = document.getElementById("institutes-content");
  const managementContent = document.getElementById("management-content");
  const modal = document.getElementById("management-modal");
  const form = document.getElementById("management-form");
  const toast = document.getElementById("toast");
  let currentView = null;
  let selectedClass = null;
  let toastTimer;

  const el = (tag, className, text) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  };

  async function request(path, options = {}) {
    const response = await fetch(api + path, {
      ...options,
      headers: { Accept: "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers }
    });
    if (!response.ok) {
      let message = `Request failed (${response.status}).`;
      try {
        const problem = await response.json();
        message = problem.detail || problem.title || message;
      } catch {
        // Keep the HTTP status message when the server does not return a problem document.
      }
      throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
  }

  function showMessage(message, isError = false) {
    window.clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.hidden = false;
    toastTimer = window.setTimeout(() => { toast.hidden = true; }, 4000);
  }

  async function refreshCache() {
    const paths = {
      institutes: "/institutes",
      courseLevels: "/course-levels",
      courses: "/courses",
      classes: "/classes",
      students: "/students",
      books: "/library/books",
      loans: "/library/loans"
    };
    const entries = await Promise.all(Object.entries(paths).map(async ([key, path]) => [key, await request(path)]));
    for (const [key, value] of entries) cache[key] = value;
  }

  function addCell(row, value) {
    row.append(el("td", "", value === null || value === undefined || value === "" ? "—" : String(value)));
  }

  function renderTable(data, config) {
    const head = document.getElementById("management-head");
    const body = document.getElementById("management-rows");
    head.replaceChildren();
    body.replaceChildren();
    const headerRow = el("tr");
    for (const [label] of config.columns) headerRow.append(el("th", "", label));
    if (config.action) headerRow.append(el("th", "", "Actions"));
    head.append(headerRow);

    const query = document.getElementById("management-search").value.trim().toLowerCase();
    const visible = data.filter((item) => config.columns.some(([, key]) =>
      item[key] !== null && item[key] !== undefined && String(item[key]).toLowerCase().includes(query)));
    if (!visible.length) {
      const row = el("tr");
      const cell = el("td", "empty-state");
      cell.colSpan = config.columns.length + (config.action ? 1 : 0);
      cell.append(el("strong", "", data.length ? "No matching records" : `No ${config.title.toLowerCase()} yet`));
      cell.append(document.createTextNode(data.length ? "Try another search." : "Use the Add button to create the first record."));
      row.append(cell);
      body.append(row);
    } else {
      for (const item of visible) {
        const row = el("tr");
        for (const [, key] of config.columns) {
          let value = item[key];
          if (key === "status") value = item.returnedAt ? "Returned" : "Checked out";
          if (key === "dueDate" && value) value = value.slice(0, 10);
          addCell(row, value);
        }
        if (config.action) {
          const actionsCell = el("td");
          const button = el("button", "secondary-button", config.action);
          button.type = "button";
          button.addEventListener("click", () => config.action === "Manage" ? openClass(item) : returnLoan(item));
          actionsCell.append(button);
          row.append(actionsCell);
        }
        body.append(row);
      }
    }
    document.getElementById("management-count").textContent =
      `Showing ${visible.length} of ${data.length} ${data.length === 1 ? "record" : "records"}`;
  }

  async function loadView(view) {
    currentView = view;
    selectedClass = null;
    document.getElementById("class-details").hidden = true;
    const config = views[view];
    document.getElementById("management-title").textContent = config.title;
    document.getElementById("management-subtitle").textContent = config.description;
    document.getElementById("management-table-title").textContent = config.title;
    document.getElementById("management-table-description").textContent = config.description;
    document.getElementById("management-stat-name").textContent = config.title;
    document.getElementById("management-stat-label").textContent =
      view === "books" ? "Catalog titles" : view === "loans" ? "Total loans" : "Total records";
    document.getElementById("management-stat-detail-label").textContent =
      view === "books" ? "Available copies" : view === "loans" ? "Checked out" : "Search results";
    document.getElementById("management-add").textContent = view === "loans"
      ? "Check out book" : view === "classes" ? "Add class" : `Add ${config.title.replace(/s$/, "").toLowerCase()}`;
    document.getElementById("management-search").value = "";
    document.getElementById("management-footer").textContent = view === "classes" ? "Select Manage to enroll students and enter scores." : "";
    document.getElementById("management-rows").replaceChildren(el("tr", "", ""));
    document.getElementById("management-count").textContent = "Loading...";
    try {
      await refreshCache();
      const data = cache[view];
      renderTable(data, config);
      document.getElementById("management-stat-total").textContent = data.length;
      const detail = view === "books"
        ? data.reduce((sum, book) => sum + book.availableCopies, 0)
        : view === "loans" ? data.filter((loan) => !loan.returnedAt).length : data.length;
      document.getElementById("management-stat-detail").textContent = detail;
    } catch (error) {
      document.getElementById("management-count").textContent = "Could not load records";
      showMessage(error.message || "Could not load this workspace.", true);
    }
  }

  function appendField(field, fieldsContainer) {
    const wrapper = el("div", "form-field");
    const label = el("label", "", field.label);
    label.htmlFor = `management-field-${field.name}`;
    wrapper.append(label);
    let input;
    if (field.type === "select") {
      input = document.createElement("select");
      input.append(new Option(`Select ${field.label.toLowerCase()}`, ""));
      for (const option of cache[field.source] || []) {
        let label = option[field.text];
        if (field.source === "courseLevels") label += ` · ${option.instituteName}`;
        if (field.source === "courses") label += ` · ${option.levelName} · ${option.instituteName}`;
        if (field.source === "students") label += ` (${option.studentNumber})`;
        if (field.source === "books") label += ` (${option.isbn} · ${option.instituteName})`;
        input.append(new Option(label, option[field.value]));
      }
    } else {
      input = document.createElement("input");
      input.type = field.type || "text";
      if (field.maxLength) input.maxLength = field.maxLength;
      if (field.min !== undefined) input.min = field.min;
      if (field.step) input.step = field.step;
      if (field.placeholder) input.placeholder = field.placeholder;
    }
    input.id = `management-field-${field.name}`;
    input.name = field.name;
    input.required = Boolean(field.required);
    wrapper.append(input);
    fieldsContainer.append(wrapper);
  }

  function openCreateForm() {
    if (currentView === "classes" && !cache.courses.length) {
      showMessage("Create a course before adding a class.", true);
      return;
    }
    if (currentView === "courses" && !cache.courseLevels.length) {
      showMessage("Create a course level before adding a course.", true);
      return;
    }
    if (["courseLevels", "students", "books"].includes(currentView) && !cache.institutes.length) {
      showMessage("Add an institute before creating this record.", true);
      return;
    }
    if (currentView === "loans" && !cache.books.some((book) => book.availableCopies > 0)) {
      showMessage("Add a book with available copies before checking one out.", true);
      return;
    }
    if (currentView === "loans" && !cache.students.length) {
      showMessage("Add a student before creating a library loan.", true);
      return;
    }
    form.reset();
    const config = views[currentView];
    document.getElementById("management-modal-title").textContent =
      currentView === "loans" ? "Check out a book" : currentView === "classes"
        ? "Add class" : `Add ${config.title.replace(/s$/, "").toLowerCase()}`;
    document.getElementById("management-modal-description").textContent = config.description;
    document.getElementById("management-form-error").hidden = true;
    const fields = document.getElementById("management-form-fields");
    fields.replaceChildren();
    config.fields.forEach((field) => appendField(field, fields));
    if (currentView === "loans") {
      const dueDate = form.elements.namedItem("dueDate");
      const defaultDueDate = new Date();
      defaultDueDate.setDate(defaultDueDate.getDate() + 14);
      dueDate.value = `${defaultDueDate.getFullYear()}-${String(defaultDueDate.getMonth() + 1).padStart(2, "0")}-${String(defaultDueDate.getDate()).padStart(2, "0")}`;
    }
    modal.hidden = false;
    fields.querySelector("input,select")?.focus();
  }

  function closeForm() {
    modal.hidden = true;
  }

  async function saveRecord(event) {
    event.preventDefault();
    const config = views[currentView];
    const payload = Object.fromEntries(new FormData(form).entries());
    if (currentView === "books") payload.totalCopies = Number(payload.totalCopies);
    const save = document.getElementById("management-save");
    save.disabled = true;
    try {
      await request(config.path, { method: "POST", body: JSON.stringify(payload) });
      closeForm();
      showMessage(currentView === "loans" ? "Book checked out." : currentView === "classes"
        ? "Class added." : `${config.title.replace(/s$/, "")} added.`);
      await loadView(currentView);
    } catch (error) {
      const errorNode = document.getElementById("management-form-error");
      errorNode.textContent = error.message || "Could not save this record.";
      errorNode.hidden = false;
    } finally {
      save.disabled = false;
    }
  }

  async function returnLoan(loan) {
    if (loan.returnedAt) return;
    try {
      await request(`/library/loans/${encodeURIComponent(loan.id)}/return`, { method: "POST" });
      showMessage("Book returned.");
      await loadView("loans");
    } catch (error) {
      showMessage(error.message || "Could not return this book.", true);
    }
  }

  async function openClass(courseClass) {
    selectedClass = courseClass;
    document.getElementById("class-details").hidden = false;
    document.getElementById("class-detail-subtitle").textContent =
      `${courseClass.instituteName} · ${courseClass.courseName} · ${courseClass.name} · ${courseClass.academicYear}`;
    try {
      await renderClassDetails();
      document.getElementById("class-details").scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (error) {
      showMessage(error.message || "Could not load class details.", true);
    }
  }

  async function renderClassDetails() {
    if (!selectedClass) return;
    const classId = encodeURIComponent(selectedClass.id);
    const [enrolled, students, assessments, scores] = await Promise.all([
      request(`/classes/${classId}/students`),
      request("/students"),
      request(`/classes/${classId}/assessments`),
      request(`/classes/${classId}/scores`)
    ]);
    const select = document.getElementById("enroll-student");
    select.replaceChildren(new Option("Select a student", ""));
    const enrolledIds = new Set(enrolled.map((student) => student.studentId));
    for (const student of students.filter((entry) => !enrolledIds.has(entry.id))) {
      select.append(new Option(`${student.name} (${student.studentNumber})`, student.id));
    }
    document.getElementById("enroll-form").querySelector("button").disabled = select.options.length < 2;
    const head = document.getElementById("class-score-head");
    const body = document.getElementById("class-score-rows");
    head.replaceChildren();
    body.replaceChildren();
    const header = el("tr");
    header.append(el("th", "", "Student"), el("th", "", "Number"));
    for (const assessment of assessments) {
      header.append(el("th", "", `${assessment.name} / ${assessment.maxScore}`));
    }
    header.append(el("th", "", "Graded total"));
    head.append(header);
    const rowsByStudent = new Map();
    for (const score of scores) {
      if (!rowsByStudent.has(score.studentId)) {
        rowsByStudent.set(score.studentId, { studentNumber: score.studentNumber, name: score.studentName, scores: new Map() });
      }
      rowsByStudent.get(score.studentId).scores.set(score.assessmentId, score.score);
    }
    for (const student of enrolled) {
      if (!rowsByStudent.has(student.studentId)) {
        rowsByStudent.set(student.studentId, { studentNumber: student.studentNumber, name: student.name, scores: new Map() });
      }
    }
    if (!rowsByStudent.size) {
      const row = el("tr");
      const cell = el("td", "empty-state", "Enroll students to start recording scores.");
      cell.colSpan = assessments.length + 3;
      row.append(cell);
      body.append(row);
    }
    for (const [studentId, student] of rowsByStudent) {
      const row = el("tr");
      addCell(row, student.name);
      addCell(row, student.studentNumber);
      for (const assessment of assessments) {
        const cell = el("td");
        const input = document.createElement("input");
        input.type = "number";
        input.min = "0";
        input.max = assessment.maxScore;
        input.step = "0.01";
        input.value = student.scores.get(assessment.id) ?? "";
        input.setAttribute("aria-label", `${assessment.name} score for ${student.name}`);
        const button = el("button", "secondary-button", "Save");
        button.type = "button";
        button.style.marginLeft = "6px";
        button.addEventListener("click", async () => {
          if (input.value === "" || Number(input.value) > Number(assessment.maxScore)) {
            showMessage("Enter a score from 0 to the assessment maximum.", true);
            return;
          }
          try {
            await request(`/assessments/${encodeURIComponent(assessment.id)}/scores`, {
              method: "PUT",
              body: JSON.stringify({ studentId, score: Number(input.value) })
            });
            showMessage("Score saved.");
            await renderClassDetails();
          } catch (error) {
            showMessage(error.message || "Could not save this score.", true);
          }
        });
        cell.append(input, button);
        row.append(cell);
      }
      const graded = assessments.filter((assessment) => student.scores.has(assessment.id)
        && student.scores.get(assessment.id) !== null);
      const earned = graded.reduce((sum, assessment) => sum + Number(student.scores.get(assessment.id)), 0);
      const possible = graded.reduce((sum, assessment) => sum + Number(assessment.maxScore), 0);
      addCell(row, possible ? `${earned.toFixed(2)} / ${possible.toFixed(2)} (${(earned / possible * 100).toFixed(1)}%)` : "—");
      body.append(row);
    }
  }

  document.querySelectorAll(".nav-item[data-view]").forEach((button) => {
    button.addEventListener("click", async () => {
      document.querySelectorAll(".nav-item[data-view]").forEach((item) => {
        item.classList.toggle("active", item === button);
        if (item === button) item.setAttribute("aria-current", "page");
        else item.removeAttribute("aria-current");
      });
      const view = button.dataset.view;
      const institutes = view === "institutes";
      mainContent.hidden = !institutes;
      managementContent.hidden = institutes;
      document.querySelector(".breadcrumb strong").textContent = institutes ? "Institutes" : views[view].title;
      if (!institutes) await loadView(view);
    });
  });
  document.getElementById("management-add").addEventListener("click", openCreateForm);
  document.getElementById("management-search").addEventListener("input", () => {
    if (currentView) renderTable(cache[currentView] || [], views[currentView]);
  });
  document.getElementById("management-modal-close").addEventListener("click", closeForm);
  document.getElementById("management-modal-cancel").addEventListener("click", closeForm);
  modal.addEventListener("click", (event) => { if (event.target === modal) closeForm(); });
  form.addEventListener("submit", saveRecord);
  document.getElementById("enroll-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await request(`/classes/${encodeURIComponent(selectedClass.id)}/students`, {
        method: "POST",
        body: JSON.stringify({ studentId: document.getElementById("enroll-student").value })
      });
      showMessage("Student enrolled.");
      await renderClassDetails();
    } catch (error) {
      showMessage(error.message || "Could not enroll this student.", true);
    }
  });
  document.getElementById("assessment-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await request(`/classes/${encodeURIComponent(selectedClass.id)}/assessments`, {
        method: "POST",
        body: JSON.stringify({
          name: document.getElementById("assessment-name").value,
          maxScore: Number(document.getElementById("assessment-max").value)
        })
      });
      event.target.reset();
      showMessage("Assessment added.");
      await renderClassDetails();
    } catch (error) {
      showMessage(error.message || "Could not add this assessment.", true);
    }
  });
  document.addEventListener("keydown", (event) => { if (event.key === "Escape" && !modal.hidden) closeForm(); });
})();
