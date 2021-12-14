# Check your State Pension Frontend

This service provides the frontend endpoint for the [Check your State Pension] project, formally known as the National Insurance and State Pension project.

Other components:
- [State Pension Backend](https://github.com/hmrc/state-pension)
- [National Insurance Backend](https://github.com/hmrc/national-insurance-record)

## Summary

This service provides the following useful information to the customer:

- When they will reach State Pension age
- How much their State Pension is currently worth
- A forecast of what their State Pension will be when they reach State Pension age
- A view of their National Insurance record, including any gaps

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), and needs at least a [JRE] version 8 to run.

## Authentication

This customer logs into this service using [GOV.UK Verify](https://www.gov.uk/government/publications/introducing-govuk-verify/introducing-govuk-verify) and the [Government Gateway](https://www.gov.uk/government-gateway)

## Testing

Rather than using IDs or classes, which can change when frameworks are invariably updated, we have switched to using data attributes. Here’s an example, using the page heading pattern.

### Component

We’ve added `data-component` hooks to the component’s semantic elements, and then appropriate attributes to identify the status of the non-semantic elements. These are purposefully using snakecase to avoid visual confusion with the class names.

```scala
<header class="hmrc-page-heading" data-component="nisp_page_heading"
  @if(specId){ data-spec="@specId"}>
  <h1 class="@classes.getOrElse("govuk-heading-xl")""
    data-component="nisp_page_heading__h1" @elmId.map { i => id="@{i}"}>
    @messages(text)
  </h1>
  <p class="govuk-caption-xl hmrc-caption-xl" data-component="nisp_page_heading__p">
    <span data-aria-hidden aria-hidden="true">
      @messages(section)
    </span>
    <span data-visually-hidden class="govuk-visually-hidden">
      @messages(context)
    </span>
  </p>
</header>
```

### View file

Call the component in the view file and add the new `specId` parameter:

```scala
  @nispPageHeading(
    text = messages("nisp.main.h1.title"),
    section = messages(user.name.getOrElse("")),
    context = messages("nisp.nirecord.context"),
    specId = Some("state_pension__pageheading")
  )
```

### Spec file

Use the component’s `specId` attribute as scope to target the inner elements.

```scala
"render page with heading 'Your State Pension' and context " in {
  mockSetup
  assertEqualsMessage(
    doc,
    "[data-spec='state_pension__pageheading'] [data-visually-hidden]",
    "nisp.nirecord.context"
  )
}
```

## Acronyms

In the context of this application we use the following acronyms and define their
meanings. Provided you will also find a web link to discover more about the systems
and technology.

- [API]: Application Programming Interface
- [HoD]: Head of Duty
- [JRE]: Java Runtime Environment
- [JSON]: JavaScript Object Notation
- [NI]: National Insurance
- [SP]: State Pension
- [NINO]: National Insurance Number
- [URL]: Uniform Resource Locator

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

[NPS]: http://www.publications.parliament.uk/pa/cm201012/cmselect/cmtreasy/731/73107.htm
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[NINO]: http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm
[NI]: https://www.gov.uk/national-insurance/overview
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html
[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator
[SP]: https://www.gov.uk/new-state-pension/overview
[JSON]: http://json.org/
[Check your State Pension]:https://www.gov.uk/check-state-pension
