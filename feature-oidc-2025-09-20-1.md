ok lets plan next set of features and add them to github actions. in assence i would like proxima to support full oidc
  authentication spec starting with code flow. essentially after redirecting to proxima it would resume flow like user has
  properly authenticated but instead of login you would at the right place of the flow return default user token. for this allow
  subject and additional claims to be defined in configuration and also allow switching between user presets. token should be
  cached as it is right now and injected in authorization bearer token - difference is that the preset will now also include
  subject and claims. default expiry will be 3600 but also configurable in presets. on startup new default token will be
  generated. for now we do not need to support other oauth flows but at the end of this both id and authorization token should
  be supported (add this at the end with additional claims and info that can be added to authorization token). develop plan for
  this and capture it as a set of github issues. be as detailed as possible and provide instruction to develop unit tests first
  together with required empty method implementations so that first pass of testing fails. then implement minimal set of code to
  pass the test. whole trick is developing as detailed set of test requirements where most of them should be implemented as
  unit test but also subset should be e2e test running on the real infrastucture and executed using playwright. when development
  of those features is done there should also be a comprehensive test bed simplifying regression testing. now proceed creating
  right set of gihub issues and document this on github wiki. you should have access to gh from command line.