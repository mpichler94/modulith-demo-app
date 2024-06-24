package io.github.mpichler94.modulith_app.pet.application

import io.github.mpichler94.modulith_app.pet.domain.Owner
import io.github.mpichler94.modulith_app.pet.domain.OwnerRepository
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.UUID

@Controller("/owners")
class OwnerController(private val repository: OwnerRepository) {


    @PostMapping("/new")
    fun processCreationForm(@Valid owner: Owner, result: BindingResult, redirectAttributes: RedirectAttributes): String {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "There was an error in creating the new owner.")
            return "owners/createOrUpdateOwnerForm"
        }

        repository.save(owner)
        redirectAttributes.addFlashAttribute("message", "New Owner Created")
        return "redirect:/owners/${owner.id}"
    }

    @GetMapping("/find")
    fun initFindForm(): String {
        return "owners/findOwners"
    }

    @GetMapping
    fun processFindForm(@RequestParam(defaultValue = "1") page: Int, owner: Owner, result: BindingResult, model: Model): String {
        val ownersResults = findPaginatedForOwnersLastName(page, owner.lastName)
        if (ownersResults.isEmpty) {
            result.rejectValue("lastName", "notFound", "not found")
            return "owners/findOwners"
        }

        if (ownersResults.totalElements == 1L) {
            return "redirect:/owners/${ownersResults.first().id}"
        }

        return addPaginationModel(page, model, ownersResults)
    }

    private fun addPaginationModel(page: Int, model: Model, paginated: Page<Owner>): String {
        val listOwners = paginated.content
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", paginated.totalPages)
        model.addAttribute("totalItems", paginated.totalElements)
        model.addAttribute("listOwners", listOwners)
        return "owners/ownersList"
    }

    private fun findPaginatedForOwnersLastName(page: Int, lastName: String) : Page<Owner> {
        val pageSize = 5
        val pageable = PageRequest.of(page - 1, pageSize)
        return repository.findByLastName(lastName, pageable)
    }

    @GetMapping("/{ownerId}/edit")
    fun initUpdateOwnerForm(ownerId: UUID, model: Model): String {
        val owner = repository.findById(ownerId)
        if (owner != null) {
            model.addAttribute(owner)
        }

        return "owners/createOrUpdateOwnerForm"
    }

    @PostMapping("/{ownerId}/edit")
    fun processUpdateOwnerForm(@Valid owner: Owner, result: BindingResult, ownerId: UUID, redirectAttributes: RedirectAttributes): String {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner")
            return "owners/createOrUpdateOwnerForm"
        }

        owner.setId(ownerId)
        repository.save(owner)
        redirectAttributes.addFlashAttribute("message", "Owner Values Updated")
        return "redirect:/owners/{ownerId}"
    }

    @GetMapping("/{ownerId}")
    fun showOwner(ownerId: UUID): ModelAndView {
        val mav = ModelAndView("owners/ownerDetails")
        val owner = repository.findById(ownerId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        mav.addObject(owner)
        return mav
    }
}