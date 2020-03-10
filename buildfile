require 'buildr/single_intermediate_layout'
require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'

desc 'A GWT linker that generates a serviceworker'
define 'gwt-serviceworker-linker' do
  project.group = 'org.realityforge.gwt.serviceworker'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/gwt-serviceworker-linker')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.provided_dependencies.concat [:javax_annotation, :gwt_user, :gwt_dev]

  compile.with :javax_annotation, :gwt_user, :gwt_dev

  test.using :testng
  test.with :mockito

  package(:jar)
  package(:sources)
  package(:javadoc)
end
