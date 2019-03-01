# Contributing

This is an open source project. We appreciate any help from the community to improve the mod.

### Bugs or ideas for new items

Did you found a bug or do you have an idea how to improve the mod? We are happy to hear from you.

- [Discord](https://discord.gg/YjK2JAD)
- [IRC Channel](https://qchat.rizon.net/?channels=electricalage)
- [Bug Tracker](https://github.com/Electrical-Age/ElectricalAge/issues)

Contributions via [pull request](https://github.com/Electrical-Age/ElectricalAge/pulls),
and [bug reports](https://github.com/Electrical-Age/ElectricalAge/issues) are welcome!
Please submit your pull request to the `develop` branch and use the GitHub issue tracker to report issues.

### Translations

Is the mod not available in your language or are some translations missing?
Well you can change that by adding or modifying a translation:

We use Transifex as localization platform. Create an account and ask to join the
[ELN Team](https://www.transifex.com/electrical-age/eln/) on Transifex to translate the mod into your language. No
coding skills are required at all.

Some translation strings might contain placeholders for runtime arguments in order to include numbers or other runtime
objects into the sentence. These are identified by **%N$** whereas *N* is the number of the argument in the argument
list (at runtime). A translation string should include these placeholders too at the appropriate position in the text.

# Development

The default branch for the Electrical Age source repository on github is **"master"**, while there is another important
branch called **"develop"**. Each of them serves his own purpose.

### master branch
The **"master"** branch is a stable branch, and gets updated only on releases. Whenever people checkout the **"master"**
branch, they get the source code of the latest release of the Electrical Age mod.

### develop branch
The **"develop"** branch, is where commits during development are integrated into. It is where the Electrical Age team
pushes or merges their actual changes together and where contributions from the community (Pull requests) are
integrated into the development version of the mod. Anyone who wish to try the cutting edge version of Electrical Age
can download the develop branch and build the mod himself.

Pull requests are always merged into the **"develop"** branch. If you are willing to contribute, make sure sending us
pull requests against the develop branch but not the *master* branch. Unlike a lot of projects **"develop"** is the main branch on GitHub, so this is the default if you don't go out of your way to change it.


### GIT Flow
They are named *master* and *develop* because most of the core developers adapt the git flow convention when working
on Electrical Age. When working on a feature that is likely taking quite some time to finish, a local feature branch is
created, and not necessarily pushed to the github. This way, when there are pending pull requests, they do not have to
wait too long, since they can be merged into develop branch first.

An introduction of git-flow can be found [here](http://nvie.com/posts/a-successful-git-branching-model/) or
[here](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

You do not necessarily have to adopt git-flow for yourself in order to contribute to the mod, as long as your changes
 use the branch **"develop"** as a base and the pull request is against the **"develop"** branch, we will be able
  to integrate your changes easily.

#### In short for EA:

- Features get developed on **feature branches**, either in your local repository or pushed to github. Feature branches
can be rebased.
- Once ready, **feature branches** are PR'd to **develop**.
- When the EA team wants to make a release, **develop** is branched into a **release branch**. Any necessary
stabilization works happen there; **develop** is never frozen, and efforts to PR in **feature branches** should not
stop just because a release is happening.
- When a release is made, the **release branch** is pushed to **master** and **master** is tagged at that point.
- If hotfixes need to be made, then they can be made on the **release branch**. Anything that also applies to the
**development branch** is first committed there, then cherry-picked to the **release branch**. The **release branch**
is then pushed to **master** (again), and tagged (again), going from e.g. 0.52.0 to 0.52.1.
- **develop**, **master** and the **release branches** are all protected; no rebasing happens there.
- If you want to test multiple features together, you should still develop them separately. You can create a separate combined branch in your own repository that you regularly merge the feature branches into. Do not attempt to PR the combo branch.
- If you want to develop one feature on top of another, then make sure you PR the 'base' feature before the derived one. If you send us a too-large PR we will ask you to chop it up.
