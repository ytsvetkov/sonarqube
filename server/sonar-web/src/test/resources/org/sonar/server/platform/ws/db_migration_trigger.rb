require 'database_version'

def call_upgrade_and_start
  DatabaseVersion.load_java_web_services
end