package uk.gov.beis.scorecard.loader

sealed trait Binding

case class StringBinding(fieldName: String) extends Binding
case class NumberBinding(fieldName: String) extends Binding

case class StructureBinding(mapping: Seq[(Binding, String)]) extends Binding

object FieldMappings {

  def s(name: String): Binding = StringBinding(name)
  def n(name: String): Binding = NumberBinding(name)

  def struct(mapping: Seq[(Binding, String)]): Binding = StructureBinding(mapping)

  val learnerStatsMappings: Seq[(Binding, String)] = Seq(
    n("learner_satisfaction_score") -> "satisfaction",
    n("learner_satisfaction_score_avg") -> "national_satisfaction",
    n("learners_age_under_19") -> "age_under_19",
    n("learners_age_19") -> "age_19_to_24",
    n("learners_age_25_plus") -> "age_25_plus",
    n("learners_total") -> "total",
    n("apprentices_intermediate") -> "intermediate",
    n("apprentices_advanced") -> "advanced",
    n("apprentices_higher") -> "higher"
  )

  val earningsMappings: Seq[(Binding, String)] = Seq(
    n("median_earnings") -> "median",
    n("proportion_earning_above_21k") -> "proportion_above_21k"
  )

  def addSuffix(bindings: Seq[(Binding, String)], suffix: String): Seq[(Binding, String)] = bindings.map {
    case (StringBinding(fieldName), jsonName) => s(s"${fieldName}_avg") -> jsonName
    case (NumberBinding(fieldName), jsonName) => n(s"${fieldName}_avg") -> jsonName
    case (StructureBinding(bs), jsonName) => struct(addSuffix(bs, suffix)) -> jsonName
  }

  val nationalEarningsMappings: Seq[(Binding, String)] = addSuffix(earningsMappings, "_avg")

  val qualStatsMappings: Seq[(Binding, String)] = Seq(
    n("pass_rate") -> "pass_rate",
    n("retention_rate") -> "retention_rate",
    n("achievement_rate") -> "achievement_rate"
  )

  val nationalQualStatsMappings: Seq[(Binding, String)] = addSuffix(qualStatsMappings, "_avg")

  val addressMappings: Seq[(Binding, String)] = Seq(
    s("address_1") -> "address1",
    s("address_2") -> "address2",
    s("town") -> "town",
    s("county") -> "county",
    s("postcode") -> "postcode",
    s("latitude") -> "latitude",
    s("longitude") -> "longitude"
  )

  val apprenticeshipMappings: Seq[(Binding, String)] = Seq(
    s("ssa_tier_2_code") -> "subject_tier_2_code",
    s("ssa_tier_2_description") -> "subject_tier_2_title",
    s("ssa_tier_2_description") -> "description",
    struct(learnerStatsMappings) -> "learner_stats",
    n("ukprn") -> "provider_id",
    struct(qualStatsMappings) -> "stats",
    struct(nationalQualStatsMappings) -> "national_stats",
    struct(earningsMappings) -> "earnings",
    struct(nationalEarningsMappings) -> "national_earnings",
    s("average_cost") -> "average_cost"
  )

  val providerMappings: Seq[(Binding, String)] = Seq(
    n("ukprn") -> "ukprn",
    s("provision_type") -> "provision_type",
    s("level") -> "level",
    s("provider_name") -> "name",
    s("provider_type") -> "provider_type",
    s("provider_region") -> "region",
    s("provider_lea") -> "lea",
    s("provider_la") -> "la",
    s("website") -> "website",
    struct(addressMappings) -> "address"
  )

}
