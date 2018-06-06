package js.form.field;

import js.form.field.Field.FieldProps;
import js.form.Form.FormProps;
import js.html.Element;
import js.html.Event;

using goog.string.GoogString;

class FormListField extends Field {
  static public var REG = 'formList';

  // ------------------------------- Events -------------------------------

  public static inline var AddFormEvent = 'add-form';
  public static inline var AddRemoveEvent = 'add-remove';

  // ------------------------------- Class -------------------------------

  var subProps: FormProps;
  var defaultItems: Int;
  var minItems: Null<Int>;
  var maxItems: Null<Int>;
  var uniqueBy: Array<String>;

  var lastSubId: Int = 1;

  public var forms(default, null): Array<Form> = [];

  var templateTag: Tag;
  var listTag: Tag;
  var adders: Array<Tag>;

  public function new(form: Form, props: FormListFieldProps) {
    super(form, props);
    subProps = props.sub;
    defaultItems = G.or(props.defaultItems, function() return 0);
    minItems = props.minItems;
    maxItems = props.maxItems;
    uniqueBy = props.uniqueBy;

    templateTag = Tag.find('#' + templateId(props));
    G.require(templateTag != null, 'Subform template "#${templateId(props)}" not found"');

    var topForm = form; // Вычислить самую верхнюю форму
    while (topForm.parentForm != null) topForm = topForm.parentForm;

    listTag = topForm.tag.fnd('#' + listId());
    G.require(listTag != null, 'Subform list placeholder "topForm #${listId()}" not found');

    adders = topForm.tag.fndAll('#' + addId());
    for (adder in adders) adder.onClick(function(e: Event) {
      addForm(true);
      e.preventDefault();
    });
  }

  // ------------------------------- Attribute names -------------------------------

  public function dataTemplateAttr(): String return "data-template";

  // ------------------------------- All id functions -------------------------------

  public function makeId(suffix: String): String return
    htmlId + '-' + suffix; // + (form.subId != null ? '-' + form.subId : '');

  public function templateId(props: FormListFieldProps): String return props.templateId != null ? props.templateId : htmlId + '-template';

  public function listId(): String return makeId('list');

  public function addId(): String return makeId('add');

  public function removeId(): String return 'form__remove';

  // ------------------------------- Field methods -------------------------------

  override public function initTag(): Tag return form.tag.fnd('#' + listId());

  override public function resetError() {
    super.resetError();
    for (form in forms) form.resetErrors();
  }

  override public function setValueEl(value: Null<Dynamic>) {
    setValueEl2(value);
  }

  public function setValueEl2(value: Null<Array<External>>) {
    forms = [];
    listTag.removeChildren();
    if (value != null && value.length > 0) {
      for (formValue in value) {
        var form = addForm(true, true, true);
        form.fill(formValue);
      }
    } else {
      for (idx in 0...defaultItems) {
        addForm(true);
      }
    }
  }

  override public function value(): Dynamic return value2();

  public function value2(): Array<External> {
    var ret: Array<External> = [];
    for (idx in 0...forms.length) {
      var form = forms[idx];
      if ((minItems != 0 && idx < minItems) || !form.isInitialEmpty()) {
        // Условие непустой формы !form.isInitialEmpty() работает только для необязательных подформ (индекс которых больше чем minItems).
        ret.push(form.value());
      }
    }
    return ret;
  }

  function makeSubFormId(subId: Int): String return htmlId + '-' + subId;

  /*
  Create new sub-form via template cloning
   */
  function newFormEl(subId: Int, vis: Bool): Tag {
    var subFormId = makeSubFormId(subId);
    var idTemplate: String = templateTag.getAttr(dataTemplateAttr());
    var formTag: Tag = templateTag.clone(true).id(subFormId);
    if (vis) formTag.clsOff(form.config.hiddenClass);
    updateSubFormChildrenTagsRecursive(formTag.el, subFormId, idTemplate);
    return formTag;
  }

  /*
  Update attributes in all inner elements of `el` for cloned sub-form
   */
  function updateSubFormChildrenTagsRecursive(el: Element, subFormId: String, idTemplate: String) {
    var children = el.children;
    for (i in 0...children.length) {
      var subEl: Element = children.item(i);
      updateSubFormChildTag(subEl, subFormId, idTemplate);
      if (!subEl.hasAttribute(dataTemplateAttr())) { // skip inner sub-form templates
        updateSubFormChildrenTagsRecursive(subEl, subFormId, idTemplate);
      }
    }
  }

  /*
  Update attributes for one element in cloned sub-form
   */
  function updateSubFormChildTag(el: Element, subFormId: String, idTemplate: String) {
    function processAttr(attrName: String): Bool {
      var v = el.getAttribute(attrName);
      if (G.toBool(v) && v.startsWith(idTemplate)) {
        v = subFormId + v.substr(idTemplate.length);
        el.setAttribute(attrName, v);
        return true;
      }
      return false;
    }

    processAttr('id');
    if (processAttr('name')) {
//      t.on('focus', function() {
//        // TODO: rr.form.field.FormListField.superClass_.resetError.apply(self)
//          resetError();
//      });
    }
    processAttr('for');
    processAttr('data-target');
  }

  /*
  Create and add a subform.
  When `maxItems` is defined and subform count exceeds the `maxItems` value, then subform will not be added.
  @return object Form, if subform was added, null otherwise
   */
  public function addForm(vis: Bool, ?showOptionalFields: Bool, ?ignoreAddCheck: Bool): Null<Form> {
    if (!ignoreAddCheck && !canAddForm()) return null;
    var subId: Int = lastSubId++;
    var t: Tag = newFormEl(subId, vis);
    t.addTo(listTag); // addTo() надо делать ДО создания объекта Form (иначе не работает Field.findFormBlock)
    var form = new Form(subProps, t, subId, this);
    dispatchEvent({
      type: AddFormEvent,
      form: form
    });
    form.init();
    form.showOptionalFields(showOptionalFields);
    form.triggerRules();

    for (tag in getRemoveTags(form)) tag.onClick(function(e: Event) {removeForm(form); e.preventDefault();});
    forms.push(form);
    updateAddRemoveEls();
    return form;
  }

  /*
  Add one or more default subforms.
  This method works only when no subform exists.
  */
  public function addDefaultForm(opt_count: Int = 1) {
    for (idx in 0...opt_count) {
      if (forms.length == 0) addForm(true, false);
    }
  }

  /*
  Removes the subform.
  @return true on success, false otherwise.
   */
  public function removeForm(form: Form): Bool {
    if (!canRemoveForm()) return false; // Нельзя удалять элементы, если их количество меньше minItems
    for (i in 0...forms.length) {
      if (form == forms[i]) {
        forms.splice(i, 1);
        form.tag.remove();
        updateAddRemoveEls();
        return true;
      }
    }
    throw "Cannot remove subform";
  }

  public function canAddForm(): Bool return !G.asBool(maxItems) || forms.length < maxItems;

  public function canRemoveForm(): Bool return !G.asBool(minItems) || forms.length > minItems;

  function updateAddRemoveEls() {
    var canAdd = canAddForm();
    for (adder in adders) adder.setCls(form.config.hiddenClass, !canAdd);

    var canRemove = canRemoveForm();
    for (form in forms) {
      for (tag in getRemoveTags(form)) {
        tag.setCls(form.config.hiddenClass, !canRemove);
      }
    }

    dispatchEvent({type: AddRemoveEvent});
  }

  /*
  Вернуть элементы удаления для подформы
   */
  function getRemoveTags(form: Form): Array<Tag> return form.tag.fndAll('#' + removeId());
}


class FormListFieldProps extends FieldProps {
  public var defaultItems: Int;
  @:optional public var minItems: Int;
  @:optional public var maxItems: Int;
  @:optional public var uniqueBy: Array<String>;
  public var sub: FormProps;
  @:optional public var templateId: String;
}
